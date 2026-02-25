package com.ampznetwork.herobrine.feature.template;

import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateLexer;
import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParser;
import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.feature.template.context.EmbedComponentReference;
import com.ampznetwork.herobrine.feature.template.context.TemplateContext;
import com.ampznetwork.herobrine.feature.template.visitor.SourceBodyVisitor;
import com.ampznetwork.herobrine.util.Constant;
import com.ampznetwork.herobrine.util.JdaUtil;
import com.ampznetwork.herobrine.util.MessageDeliveryTarget;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReference;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ComponentInteraction;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.kyori.adventure.text.format.NamedTextColor;
import org.antlr.v4.runtime.CodePointBuffer;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Streams;
import org.comroid.api.text.Markdown;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.discord.JdaCommandAdapter;
import org.comroid.commands.model.CommandPrivacyLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
@Component
@Command("template")
public class MessageTemplateEngine extends ListenerAdapter implements AuditLogSender {
    private static final Pattern MD_PATTERN = Pattern.compile("`{3}dmt\\n([^`]*)`{3}");

    public static final String INTERACTION_FINALIZE_IN_CHANNEL = "mte_finalize_channel";
    public static final String INTERACTION_RESEND_HERE = "mte_resend_here";

    public static final String ERROR_NO_TEMPLATE   = "No message template script found";
    public static final String ERROR_NO_REFERENCE  = "No message reference found";
    public static final String ERROR_NO_SELECTION  = "No channel was selected";
    public static final String ERROR_NO_PERMISSION = "Insufficient permissions";

    public static final Pattern SHORTCUT_REMOVE_LINE = Pattern.compile("-(\\d+)");
    public static final Pattern SHORTCUT_APPEND_LINE = Pattern.compile("\\+(\\d+) ([^\n]+)");
    public static final Pattern SHORTCUT_EDIT_LINE   = Pattern.compile("#(\\d+) ([^\n]+)");

    private final Set<InteractiveMode> interactive = new HashSet<>();

    public TemplateContext parse(String template, GenericEvent context) {
        var charBuffer  = CharBuffer.wrap(template.toCharArray());
        var buffer      = CodePointBuffer.withChars(charBuffer);
        var charStream  = CodePointCharStream.fromBuffer(buffer);
        var tokenSource = new DiscordMessageTemplateLexer(charStream);
        var tokenStream = new CommonTokenStream(tokenSource);
        var parser      = new DiscordMessageTemplateParser(tokenStream);
        var body        = SourceBodyVisitor.INSTANCE.visitSource_body(parser.source_body());
        var constants = findConstants(context);

        return new TemplateContext(body, Collections.unmodifiableMap(constants));
    }

    @Command
    @Description("Evaluate message template scripts")
    public MessageCreateBuilder evaluate(GenericInteractionCreateEvent event, @Command.Arg String template) {
        var context = parse(template, event);

        return context.evaluate().addComponents(createFinalizerActionRow());
    }

    @Command(privacy = CommandPrivacyLevel.PUBLIC)
    @Description("Start interactive template mode")
    public JdaCommandAdapter.ResponseCallback interactive(JDA jda, MessageChannel channel, User user) {
        var result = findInteractiveMode(channel, user);

        if (result.isPresent()) {
            var mode = result.get();
            return new JdaCommandAdapter.ResponseCallback(new MessageCreateBuilder().addEmbeds(new EmbedBuilder().setDescription(
                                    "Interactive mode was exited")
                            .setFooter("The complete template is contained as a file attachment to this message")
                            .build())
                    .addFiles(FileUpload.fromData(mode.buffer.toString().getBytes(StandardCharsets.UTF_8),
                            "message.dmt")), message -> {
                interactive.removeIf(it -> it.channel.equals(channel) && it.user.equals(user));
                return message.addReaction(Constant.EMOJI_EVAL_TEMPLATE).map($ -> message);
            });
        }

        return new JdaCommandAdapter.ResponseCallback(new EmbedBuilder().setDescription(
                "Send lines to append code to this template..."), message -> {
            var mode = new InteractiveMode(channel, user, message);
            interactive.add(mode);
            return new CompletedRestAction<>(jda, message);
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        var componentId = event.getComponentId();
        var message     = event.getMessage();

        if (!message.getAuthor().equals(event.getUser()) && (event.getMember() == null || !event.getMember()
                .hasPermission(Permission.MESSAGE_MANAGE))) {
            event.reply(ERROR_NO_PERMISSION).setEphemeral(true).queue();
            return;
        }

        final var channel = event.getChannel();
        switch (componentId) {
            case INTERACTION_RESEND_HERE -> {
                var reference = event.getMessage().getMessageReference();
                if (reference == null) {
                    event.reply(ERROR_NO_REFERENCE).setEphemeral(true).queue();
                    return;
                }

                event.deferReply(true)
                        .flatMap(hook -> topmostMessageByReferences(channel,
                                reference).flatMap(referenced -> hookSendFinal(event,
                                        channel,
                                        message,
                                        referenced,
                                        hook))
                                .onErrorFlatMap(JdaUtil.exceptionLogger(log,
                                        hook,
                                        "Could not respond to button interaction")))
                        .queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        if (!INTERACTION_FINALIZE_IN_CHANNEL.equals(event.getComponentId())) return;

        var channel = event.getChannel();
        var message   = event.getMessage();
        var reference = message.getMessageReference();
        if (reference == null) {
            event.reply(ERROR_NO_REFERENCE).setEphemeral(true).queue();
            return;
        }

        if (event.getValues().isEmpty()) {
            event.reply(ERROR_NO_SELECTION).queue();
            return;
        }

        event.deferReply(true)
                .flatMap(hook -> topmostMessageByReferences(channel, reference).flatMap(referenced -> {
                            var selected = event.getValues()
                                    .stream()
                                    .flatMap(Streams.cast(MessageChannelUnion.class))
                                    .findAny()
                                    .orElseThrow();

                            return hookSendFinal(event, selected, message, referenced, hook);
                        })
                        .onErrorFlatMap(JdaUtil.exceptionLogger(log,
                                hook,
                                "Could not respond to entity selection interaction")))
                .queue();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        var message = event.getMessage();

        var result = findInteractiveMode(event.getChannel(), event.getAuthor());
        if (result.isPresent()) {
            var detail = result.get();

            if (!detail.buffer.isEmpty()) detail.buffer.append('\n');

            var split = new ArrayList<>(List.of(message.getContentRaw().split("\r?\n")));
            split.removeIf(String::isBlank);

            for (var line : split) {
                Matcher matcher;
                var     lines = new ArrayList<>(List.of(detail.buffer.toString().split("\r?\n")));

                if (line.equals("!clear")) {
                    detail.buffer = new StringBuffer();
                } else if ((matcher = SHORTCUT_REMOVE_LINE.matcher(line)).matches()) {
                    var lineIndex = Integer.parseInt(matcher.group(1)) - 1;

                    lines.remove(lineIndex);

                    detail.buffer = new StringBuffer(String.join("\n", lines));
                } else if ((matcher = SHORTCUT_APPEND_LINE.matcher(line)).matches()) {
                    var lineIndex = Integer.parseInt(matcher.group(1)) - 1;
                    var append    = matcher.group(2);

                    var buf = lines.get(lineIndex);
                    lines.set(lineIndex, buf + append);

                    detail.buffer = new StringBuffer(String.join("\n", lines));
                } else if ((matcher = SHORTCUT_EDIT_LINE.matcher(line)).matches()) {
                    var lineIndex   = Integer.parseInt(matcher.group(1)) - 1;
                    var lineContent = matcher.group(2);

                    lines.set(lineIndex, lineContent);

                    detail.buffer = new StringBuffer(String.join("\n", lines));
                } else detail.buffer.append(line);
            }
            message.delete().flatMap($ -> detail.refresh(event)).queue();

            return;
        }

        var txt = findTemplate(message);
        if (txt.isEmpty()) return;

        message.addReaction(Constant.EMOJI_EVAL_TEMPLATE).queue();
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        var user = event.getUser();

        if (user == null || user.isBot()) return;
        if (!event.getReaction().getEmoji().equals(Constant.EMOJI_EVAL_TEMPLATE)) return;

        try {
            var channel = event.getChannel();

            event.retrieveMessage()
                    .flatMap(message -> sendPreview(channel,
                            event,
                            ref(message),
                            new MessageDeliveryTarget.Reply(message)))
                    .queue();
        } finally {
            event.getReaction().removeReaction(user).queue();
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private Optional<InteractiveMode> findInteractiveMode(MessageChannel channel, UserSnowflake user) {
        return interactive.stream().filter(it -> it.channel.equals(channel) && it.user.equals(user)).findAny();
    }

    private RestAction<?> hookSendFinal(
            GenericEvent event, MessageChannelUnion channel, Message original,
            Message referenced, InteractionHook hook
    ) {
        RestAction<?> action = sendFinal(channel, referenced, event, new MessageDeliveryTarget.Hook(hook));

        var flag = action instanceof MessageCreateAction;
        action = action.flatMap($ -> original.delete());
        if (flag) action = action.flatMap($ -> hook.deleteOriginal());

        return action;
    }

    private RestAction<Message> topmostMessageByReferences(MessageChannelUnion channel, MessageReference reference) {
        final var helper = new Object() {
            RestAction<Message> tryStepInto(Message msg) {
                var ref = msg.getMessageReference();
                // todo: "completed" restaction somehow?
                if (ref == null) return new CompletedRestAction<>(msg.getJDA(), msg);
                return channel.retrieveMessageById(ref.getMessageId());
            }
        };

        // todo lol this is dogshit
        return channel.retrieveMessageById(reference.getMessageId())
                .flatMap(helper::tryStepInto)
                .flatMap(helper::tryStepInto);
    }

    private RestAction<?> sendFinal(
            MessageChannelUnion channel, Message referenced, GenericEvent event,
            MessageDeliveryTarget callback
    ) {
        var template = verifyTemplate(referenced, event, callback);
        if (template.isEmpty()) return callback.send(ERROR_NO_TEMPLATE);

        var response = template.get().evaluate().build();

        newAuditEntry().level(Level.INFO)
                .guild(channel.asGuildMessageChannel().getGuild())
                .message("%s is evaluating template from %s in channel %s".formatted(referenced.getAuthor(),
                        referenced,
                        channel))
                .queue();

        return channel.sendMessage(response);
    }

    private RestAction<?> sendPreview(
            MessageChannelUnion channel, GenericEvent event, MessageReference reference,
            MessageDeliveryTarget callback
    ) {
        return channel.retrieveMessageById(reference.getMessageId()).flatMap(referenced -> {
            var template = verifyTemplate(referenced, event, callback);
            if (template.isEmpty()) return callback.send(ERROR_NO_TEMPLATE);

            var response = template.get().evaluate().addComponents(createFinalizerActionRow()).build();
            return callback.send(response);
        });
    }

    private Optional<TemplateContext> verifyTemplate(
            Message referenced, GenericEvent event, @Nullable MessageDeliveryTarget callback) {
        if (referenced == null) {
            if (callback != null) callback.send(ERROR_NO_REFERENCE).queue();
            return Optional.empty();
        }

        var txt = findTemplate(referenced);
        if (txt.isEmpty()) {
            if (callback != null) callback.send(ERROR_NO_TEMPLATE).queue();
            return Optional.empty();
        }

        return txt.map(template -> parse(template, event));
    }

    private MessageReference ref(Message message) {
        return new MessageReference(MessageReference.MessageReferenceType.DEFAULT.getId(),
                message.getIdLong(),
                message.getChannelIdLong(),
                message.getGuildIdLong(),
                message,
                message.getJDA());
    }

    private Optional<String> findTemplate(Message message) {
        if (message.getAuthor().isBot() && message.getAttachments()
                .stream()
                .map(Message.Attachment::getFileName)
                .noneMatch(name -> name.endsWith(".dmt"))) return Optional.empty();

        var content = message.getContentRaw();
        var matcher = MD_PATTERN.matcher(content);

        if (matcher.find()) return Optional.of(matcher.group(1));
        if (!message.getAttachments().isEmpty()) {
            var attachment = message.getAttachments().getFirst();
            try (
                    var is = attachment.getProxy().download().join(); var isr = new InputStreamReader(is);
                    var sw = new StringWriter()
            ) {
                isr.transferTo(sw);
                return Optional.of(sw.toString());
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not download message attachment " + attachment, e);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("DataFlowIssue")
    private Map<CharSequence, Object> findConstants(Object context) {
        return switch (context) {
            case ComponentInteraction ci -> Map.of("guild",
                    ci.getGuild(),
                    "member",
                    ci.getMember(),
                    "channel",
                    ci.getChannel(),
                    "user",
                    ci.getUser(),
                    "message",
                    ci.getMessage());
            case GenericMessageReactionEvent gmre -> Map.of("guild",
                    gmre.getGuild(),
                    "member",
                    gmre.getMember(),
                    "channel",
                    gmre.getChannel(),
                    "user",
                    gmre.getUser(),
                    "message",
                    gmre.retrieveMessage().submit().join());
            default -> Map.of();
        };
    }

    private Collection<ActionRow> createFinalizerActionRow() {
        return List.of(ActionRow.of(EntitySelectMenu.create(INTERACTION_FINALIZE_IN_CHANNEL,
                        EntitySelectMenu.SelectTarget.CHANNEL).setPlaceholder("Send this into channel...").build()),
                ActionRow.of(Button.primary(INTERACTION_RESEND_HERE, "Send here")));
    }

    @Value
    @EqualsAndHashCode(of = { "channel", "user" })
    private class InteractiveMode {
        MessageChannel channel;
        UserSnowflake  user;
        Message        infoMessage;
        @NonFinal StringBuffer buffer;

        public InteractiveMode(MessageChannel channel, UserSnowflake user, Message infoMessage) {
            this.channel     = channel;
            this.user        = user;
            this.infoMessage = infoMessage;
            this.buffer      = new StringBuffer();
        }

        public RestAction<Message> refresh(GenericEvent event) {
            return infoMessage.editMessage(JdaUtil.convertToEditData(parse(buffer.toString(), event).evaluate()
                    .addEmbeds(infoEmbed().build())
                    .build()));
        }

        public EmbedBuilder infoEmbed() {
            var embed = new EmbedBuilder().setTitle("Interactive template mode")
                    .setDescription(Markdown.CodeBlock.apply(codeWithLines(buffer.toString())))
                    .addField("Remove line",
                            "Write `-<line number>` to remove a specific line from the template",
                            false)
                    .addField("Append to line",
                            "Write `+<line number> <script>` to append to a line of the template",
                            false)
                    .addField("Modify line", "Write `#<line number> <script>` to modify a line of the template", false)
                    .addField("Clear template",
                            "Write `!clear` to clear the entire template (Warning: Cannot be undone!",
                            false)
                    .setFooter("Available shorthand commands");

            EmbedComponentReference.author.accept(embed, user);

            return embed.setColor(NamedTextColor.LIGHT_PURPLE.value());
        }

        private String codeWithLines(String string) {
            var split     = string.split("\r?\n");
            var lineCount = String.valueOf(split.length).length();
            var buf       = new StringBuilder();

            for (var i = 0; i < split.length; i++)
                buf.append(linePrefix(lineCount, i + 1)).append("  ").append(split[i]).append('\n');

            return buf.toString();
        }

        private String linePrefix(int lineCount, int lineIndex) {
            return ("%0" + lineCount + "d").formatted(lineIndex);
        }
    }
}
