package com.ampznetwork.herobrine.feature.template;

import com.ampznetwork.herobrine.feature.template.context.EmbedComponentReference;
import com.ampznetwork.herobrine.util.Constant;
import com.ampznetwork.herobrine.util.JdaUtil;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.requests.CompletedRestAction;
import net.kyori.adventure.text.format.NamedTextColor;
import org.comroid.annotations.Description;
import org.comroid.api.text.Markdown;
import org.comroid.api.tree.UncheckedCloseable;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.discord.JdaCommandAdapter;
import org.comroid.commands.model.CommandPrivacyLevel;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log
@Component
@Command("template")
@ConditionalOnBean({ MessageTemplateEngine.class })
public class InteractiveTemplateService extends ListenerAdapter {
    private static final Pattern SHORTCUT_REMOVE_LINE = Pattern.compile("-(\\d+)");
    private static final Pattern SHORTCUT_APPEND_LINE = Pattern.compile("\\+(\\d+) ([^\n]+)");
    private static final Pattern SHORTCUT_EDIT_LINE   = Pattern.compile("#(\\d+) ([^\n]+)");

    private final Set<InteractiveMode> interactive = new HashSet<>();

    @Autowired MessageTemplateEngine templateEngine;

    @Command(permission = "8192", privacy = CommandPrivacyLevel.PUBLIC)
    @Description("Start interactive template mode")
    public JdaCommandAdapter.ResponseCallback interactive(JDA jda, MessageChannel channel, User user) {
        var result = findInteractiveMode(channel, user);

        if (result.isPresent()) try (var mode = result.get()) {
            return mode.createExitCallback();
        }

        return new JdaCommandAdapter.ResponseCallback(new EmbedBuilder().setDescription(
                "Send lines to append code to this template..."), message -> {
            var mode = new InteractiveMode(channel, user, message);
            interactive.add(mode);
            return new CompletedRestAction<>(jda, message);
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        var message = event.getMessage();

        var result = findInteractiveMode(event.getChannel(), event.getAuthor());
        if (result.isEmpty()) return;

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
    }

    @Override
    public void onMessageDelete(@NonNull MessageDeleteEvent event) {
        handleMessageDelete(event.getMessageIdLong());
    }

    @Override
    public void onMessageBulkDelete(@NonNull MessageBulkDeleteEvent event) {
        event.getMessageIds().stream().mapToLong(Long::parseLong).forEach(this::handleMessageDelete);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private void handleMessageDelete(long messageId) {
        interactive.stream()
                .filter(it -> it.infoMessage.getIdLong() == messageId)
                .toList()
                .forEach(InteractiveMode::close);
    }

    private Optional<InteractiveMode> findInteractiveMode(MessageChannel channel, UserSnowflake user) {
        return interactive.stream().filter(it -> it.channel.equals(channel) && it.user.equals(user)).findAny();
    }

    @Value
    @EqualsAndHashCode(of = { "channel", "user" })
    private class InteractiveMode implements UncheckedCloseable {
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
            return infoMessage.editMessage(JdaUtil.convertToEditData(templateEngine.parse(buffer.toString(), event)
                    .evaluate()
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

        public JdaCommandAdapter.ResponseCallback createExitCallback() {
            return new JdaCommandAdapter.ResponseCallback(createExitMessage(),
                    message -> message.addReaction(Constant.EMOJI_EVAL_TEMPLATE));
        }

        private MessageCreateBuilder createExitMessage() {
            var file = FileUpload.fromData(buffer.toString().getBytes(StandardCharsets.UTF_8), "message.dmt");
            return new MessageCreateBuilder().useComponentsV2()
                    .addComponents(TextDisplay.of("## Interactive mode was exited"),
                            TextDisplay.of("The complete template is contained as a file attachment to this message"),
                            FileDisplay.fromFile(file));
        }

        @Override
        public void close() {
            try {
                var exitMessage = createExitMessage().build();

                infoMessage.editMessage(JdaUtil.convertToEditData(exitMessage)).onErrorFlatMap(t -> {
                    log.log(Level.FINE, "Could not edit info message, sending new message", t);
                    return channel.sendMessage(exitMessage);
                }).flatMap(message -> message.addReaction(Constant.EMOJI_EVAL_TEMPLATE)).queue();
            } finally {
                interactive.remove(this);
            }
        }
    }
}
