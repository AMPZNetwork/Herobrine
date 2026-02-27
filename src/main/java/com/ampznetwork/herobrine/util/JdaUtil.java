package com.ampznetwork.herobrine.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.comroid.api.Polyfill;
import org.comroid.api.java.StackTraceUtils;
import org.comroid.api.text.Markdown;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JdaUtil {
    public static <RA extends RestAction<?>> Function<Throwable, RA> exceptionLogger(
            Logger log, InteractionHook hook,
            String message
    ) {
        return exceptionLogger(log, new MessageDeliveryTarget.Hook(hook), message);
    }

    public static <RA extends RestAction<?>> Function<Throwable, RA> exceptionLogger(
            Logger log,
            MessageDeliveryTarget delivery,
            String message
    ) {
        return t -> {
            log.log(Level.SEVERE, message, t);
            return Polyfill.uncheckedCast(delivery.send("%s ```\n%s\n```".formatted(message,
                    StackTraceUtils.toString(t))));
        };
    }

    public static MessageEditData convertToEditData(MessageCreateData message) {
        var edit = new MessageEditBuilder().setReplace(true);

        if (!message.getContent().isBlank()) edit = edit.setContent(message.getContent());
        if (!message.getEmbeds().isEmpty()) edit = edit.setEmbeds(message.getEmbeds());
        if (!message.getComponents().isEmpty()) edit = edit.setComponents(message.getComponents());
        if (!message.getAllowedMentions().isEmpty()) edit = edit.setAllowedMentions(message.getAllowedMentions());
        if (!message.getAttachments().isEmpty()) edit = edit.setAttachments(message.getAttachments());

        return edit.build();
    }

    public static MessageEmbed logEntryEmbed(
            Level level, String sourceName, CharSequence message,
            @Nullable Throwable t
    ) {
        var embed = new EmbedBuilder().setTitle(sourceName).setDescription(message).setFooter(level.getName());

        if (t != null) {
            var str = StackTraceUtils.toString(t);
            embed.addField("Attached Exception", Markdown.CodeBlock.apply(str), false);
        }

        return embed.build();
    }
}
