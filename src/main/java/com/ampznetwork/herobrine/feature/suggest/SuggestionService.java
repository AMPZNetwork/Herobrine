package com.ampznetwork.herobrine.feature.suggest;

import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Debug;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.springframework.stereotype.Component;

@Log
@Component
public class SuggestionService {
    public static final long CHANNEL_ID = Debug.isDebug() ? 1141990824167624734L : 1440076272263893113L;

    @Interaction("suggest")
    @Description("Post a suggestion")
    public void suggest(
            User user,
            @Parameter(value = "suggestion") @Description("The suggestion") String suggestion
    ) {
        var jda     = user.getJDA();
        var channel = jda.getForumChannelById(CHANNEL_ID);

        if (channel == null) {
            log.warning("No suggestion channel found with ID: " + CHANNEL_ID);
            return;
        }

        channel.createForumPost("Suggestion by %s".formatted(user.getEffectiveName()),
                new MessageCreateBuilder().setContent(suggestion).build()).queue();
    }
}
