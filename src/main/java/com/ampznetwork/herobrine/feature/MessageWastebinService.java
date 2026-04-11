package com.ampznetwork.herobrine.feature;

import com.ampznetwork.herobrine.util.Constant;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Log
@Component
public class MessageWastebinService {
    @EventListener
    public void on(@NonNull MessageReceivedEvent event) {
        if (!(event.getAuthor() instanceof SelfUser)) return;

        event.getMessage().addReaction(Constant.EMOJI_DELETE).queue();
    }

    @EventListener
    public void on(@NonNull MessageReactionAddEvent event) {
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
}
