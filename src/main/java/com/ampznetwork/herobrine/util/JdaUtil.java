package com.ampznetwork.herobrine.util;

import jakarta.persistence.AttributeConverter;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.channel.GenericChannelEvent;
import net.dv8tion.jda.api.events.emoji.GenericEmojiEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.comroid.annotations.Instance;
import org.comroid.api.Polyfill;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.Markdown;
import org.comroid.api.text.StringUtil;
import org.comroid.commands.autofill.IAutoFillProvider;
import org.comroid.commands.impl.CommandUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

public class JdaUtil {
    public static <RA extends RestAction<?>> Function<Throwable, RA> exceptionLogger(Logger log, InteractionHook hook, String message) {
        return exceptionLogger(log, new MessageDeliveryTarget.Hook(hook), message);
    }

    public static <RA extends RestAction<?>> Function<Throwable, RA> exceptionLogger(Logger log, MessageDeliveryTarget delivery, String message) {
        return t -> {
            log.log(Level.SEVERE, message, t);
            return Polyfill.uncheckedCast(delivery.send("%s ```\n%s\n```".formatted(message, StackTraceUtils.toString(t))));
        };
    }

    public static MessageEditData convertToEditData(MessageCreateData message) {
        var edit = new MessageEditBuilder().setReplace(true).useComponentsV2(message.isUsingComponentsV2());

        if (!message.getContent().isBlank()) edit = edit.setContent(message.getContent());
        if (!message.getEmbeds().isEmpty()) edit = edit.setEmbeds(message.getEmbeds());
        if (!message.getComponents().isEmpty()) edit = edit.setComponents(message.getComponents());
        if (!message.getAllowedMentions().isEmpty()) edit = edit.setAllowedMentions(message.getAllowedMentions());
        if (!message.getAttachments().isEmpty()) edit = edit.setAttachments(message.getAttachments());

        return edit.build();
    }

    public static MessageEmbed logEntryEmbed(Level level, String sourceName, CharSequence message, @Nullable Throwable t) {
        var embed = new EmbedBuilder().setTitle(sourceName).setDescription(message).setFooter(level.getName());

        if (t != null) {
            var str = StackTraceUtils.toString(t);
            str = StringUtil.maxLength(str, 1000);

            embed.addField("Attached Exception", Markdown.CodeBlock.apply(str), false);
        }

        return embed.build();
    }

    public static Optional<Guild> getGuild(GenericEvent event) {
        return Optional.ofNullable(switch (event) {
            case GenericGuildEvent gge -> gge.getGuild();
            case GenericChannelEvent gce -> gce.getGuild();
            case GenericMessageEvent gme -> gme.getGuild();
            case GenericEmojiEvent gee -> gee.getGuild();
            case GenericInteractionCreateEvent gice -> gice.getGuild();
            default -> null;
        });
    }

    public static <GE extends GenericEvent> @NotNull Function<@NotNull GE, @Nullable GE> eventGuildFilter(
            final long guildId
    ) {
        return event -> getGuild(event).map(ISnowflake::getIdLong).filter(id -> id == guildId).map($ -> event).orElse(null);
    }

    public static @Nullable Message getMessage(GenericMessageEvent event) {
        return event instanceof MessageReceivedEvent mre ? mre.getMessage() : null;
    }

    public static RestAction<?> replySuccess(
            IReplyCallback callback
    ) {
        return replySuccess(callback, null);
    }

    public static RestAction<?> replySuccess(IReplyCallback callback, @Nullable Function<Message, ? extends RestAction<Message>> finalizer) {
        var action = callback.replyEmbeds(new EmbedBuilder().setTitle(Constant.EMOJI_SUCCESS.getFormatted() + " Success")
                .setColor(Constant.COLOR_SUCCESS)
                .setFooter(Constant.STRING_SELF_DESTRUCT.formatted(2))
                .build()).setEphemeral(true).map(hook -> hook.getCallbackResponse().getMessage());
        if (finalizer != null) action = action.flatMap(finalizer);
        return action.delay(2, TimeUnit.SECONDS).flatMap(Message::delete);
    }

    public enum AutoFillLogLevels implements IAutoFillProvider {
        @Instance INSTANCE;

        @Override
        public Stream<? extends CharSequence> autoFill(CommandUsage usage, String argName, String currentValue) {
            return Stream.of("OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL");
        }
    }

    private static JDA jda() {
        return bean(JDA.class);
    }

    @Value
    @NonFinal
    private static abstract class SnowflakeConverter<T extends ISnowflake> implements AttributeConverter<T, @NotNull Long> {
        @Override
        public @NotNull Long convertToDatabaseColumn(T snowflake) {
            return snowflake.getIdLong();
        }

        @Override
        public abstract T convertToEntityAttribute(@NotNull Long id);
    }

    @Value
    public static class UserConverter extends SnowflakeConverter<User> {
        @Override
        public User convertToEntityAttribute(@NotNull Long id) {
            return jda().getUserById(id);
        }
    }

    @Value
    public static class GuildConverter extends SnowflakeConverter<Guild> {
        @Override
        public Guild convertToEntityAttribute(@NotNull Long id) {
            return jda().getGuildById(id);
        }
    }

    @Value
    public static class RoleConverter extends SnowflakeConverter<Role> {
        @Override
        public Role convertToEntityAttribute(@NotNull Long id) {
            return jda().getRoleById(id);
        }
    }

    @Value
    public static class ChannelConverter extends SnowflakeConverter<ChannelUnion> {
        @Override
        public ChannelUnion convertToEntityAttribute(@NotNull Long id) {
            return jda().getChannelById(ChannelUnion.class, id);
        }
    }
}
