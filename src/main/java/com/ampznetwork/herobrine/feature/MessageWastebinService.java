package com.ampznetwork.herobrine.feature;

import com.ampznetwork.herobrine.util.Constant;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.commands.impl.CommandManager;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Log
@Component
public class MessageWastebinService extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        if (!(event.getAuthor() instanceof SelfUser)) return;

        event.getMessage().addReaction(Constant.EMOJI_DELETE).queue();
    }

    @Override
    public void onMessageReactionAdd(@NonNull MessageReactionAddEvent event) {
        var member = event.getMember();

        if (!event.getReaction().getEmoji().equals(Constant.EMOJI_DELETE)) return;
        if (member == null || event.getUser() instanceof SelfUser) return;

        var message = event.retrieveMessage().complete();
        if (!(message.getAuthor() instanceof SelfUser)) return;

        try {
            if (!member.hasPermission(Permission.MESSAGE_MANAGE)) return;

            message.delete().queue();
        } finally {
            event.getReaction().removeReaction(member.getUser()).queue();
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
