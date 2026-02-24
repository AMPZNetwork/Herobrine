package com.ampznetwork.herobrine.component.template;

import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateLexer;
import com.ampznetwork.herobrine.antlr.DiscordMessageTemplateParser;
import com.ampznetwork.herobrine.component.template.context.TemplateContext;
import com.ampznetwork.herobrine.component.template.visitor.SourceBodyVisitor;
import com.ampznetwork.herobrine.util.Constant;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.antlr.v4.runtime.CodePointBuffer;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Streams;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Log
@Component
@Command("template")
public class MessageTemplateEngine extends ListenerAdapter {
    private static final Pattern MD_PATTERN = Pattern.compile("`{3}dmt\\n([^`]*)`{3}");

    public static final String INTERACTION_EVALUATE            = "mte_evaluate";
    public static final String INTERACTION_DISMISS             = "mte_dismiss";
    public static final String INTERACTION_FINALIZE_IN_CHANNEL = "mte_finalize_channel";

    public static final String ERROR_NO_TEMPLATE  = "No message template script found";
    public static final String ERROR_NO_REFERENCE = "No message reference found";
    public static final String ERROR_NO_SELECTION = "No channel was selected";

    public TemplateContext parse(String template) {
        var charBuffer  = CharBuffer.wrap(template.toCharArray());
        var buffer      = CodePointBuffer.withChars(charBuffer);
        var charStream  = CodePointCharStream.fromBuffer(buffer);
        var tokenSource = new DiscordMessageTemplateLexer(charStream);
        var tokenStream = new CommonTokenStream(tokenSource);
        var parser      = new DiscordMessageTemplateParser(tokenStream);
        var body        = SourceBodyVisitor.INSTANCE.visitSource_body(parser.source_body());

        return new TemplateContext(body, Map.of());
    }

    @Command
    @Description("Evaluate message template scripts")
    public MessageCreateBuilder evaluate(@Command.Arg String template) {
        var context = parse(template);

        return context.evaluate().addComponents(createFinalizerActionRow());
    }

    @Override
    public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
        var componentId = event.getComponentId();
        var message     = event.getMessage();

        if (!message.getAuthor().equals(event.getUser()) && (event.getMember() == null || !event.getMember()
                .hasPermission(Permission.MESSAGE_MANAGE))) return;

        switch (componentId) {
            case INTERACTION_EVALUATE -> {
                var referenced = event.getMessage().getReferencedMessage();
                var template   = verifyTemplate(referenced, event);
                if (template.isEmpty()) {
                    event.reply(ERROR_NO_TEMPLATE).setEphemeral(true).queue();
                    return;
                }

                var response = template.get().evaluate().addComponents(createFinalizerActionRow()).build();
                event.reply(response).setEphemeral(true).queue();
            }
            case INTERACTION_DISMISS -> message.delete().queue();
        }
    }

    @Override
    public void onEntitySelectInteraction(@NonNull EntitySelectInteractionEvent event) {
        if (!INTERACTION_FINALIZE_IN_CHANNEL.equals(event.getComponentId())) return;

        var referenced = event.getMessage().getReferencedMessage();
        var template   = verifyTemplate(referenced, event);
        if (template.isEmpty()) {
            event.reply(ERROR_NO_TEMPLATE).setEphemeral(true).queue();
            return;
        }

        if (event.getValues().isEmpty()) {
            event.reply(ERROR_NO_SELECTION).setEphemeral(true).queue();
            return;
        }

        var response = template.get().evaluate().build();
        event.getValues()
                .stream()
                .flatMap(Streams.cast(MessageChannelUnion.class))
                .findAny()
                .orElseThrow()
                .sendMessage(response)
                .queue();
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        var message = event.getMessage();
        var txt     = findTemplate(message);
        if (txt.isEmpty()) return;

        message.reply(createInitMessage().build()).queue();
    }

    private Optional<TemplateContext> verifyTemplate(Message referenced, IReplyCallback callback) {
        if (referenced == null) {
            callback.reply(ERROR_NO_REFERENCE).setEphemeral(true).queue();
            return Optional.empty();
        }

        var txt = findTemplate(referenced);
        if (txt.isEmpty()) {
            callback.reply(ERROR_NO_TEMPLATE).setEphemeral(true).queue();
            return Optional.empty();
        }

        return txt.map(this::parse);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private Optional<String> findTemplate(Message message) {
        if (message.getAuthor().isBot()) return Optional.empty();

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

    private MessageCreateBuilder createInitMessage() {
        return new MessageCreateBuilder().setEmbeds(new EmbedBuilder().setTitle(
                "This message contains a template script").build()).addComponents(createInitActionRow());
    }

    private ActionRow createInitActionRow() {
        return ActionRow.of(Button.primary(INTERACTION_EVALUATE, "%s Evaluate".formatted(Constant.EMOJI_EVAL_TEMPLATE)),
                Button.danger(INTERACTION_DISMISS, "%s Dismiss".formatted(Constant.EMOJI_DELETE)));
    }

    private ActionRow createFinalizerActionRow() {
        return ActionRow.of(EntitySelectMenu.create(INTERACTION_FINALIZE_IN_CHANNEL,
                EntitySelectMenu.SelectTarget.CHANNEL).setPlaceholder("Send this into channel...").build());
    }
}
