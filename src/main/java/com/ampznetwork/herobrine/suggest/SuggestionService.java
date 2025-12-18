package com.ampznetwork.herobrine.suggest;

import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Debug;
import org.comroid.api.text.StringMode;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Log
@Component
public class SuggestionService extends ListenerAdapter {
    public static final long CHANNEL_ID = Debug.isDebug() ? 1141990824167624734L : 1440076272263893113L;

    @Command("suggest")
    @Description("Post a suggestion")
    public void suggest(
            User user,
            @Command.Arg(value = "suggestion",
                         stringMode = StringMode.GREEDY) @Description("The suggestion") String suggestion
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

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
