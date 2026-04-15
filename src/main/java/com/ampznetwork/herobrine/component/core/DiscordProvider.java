package com.ampznetwork.herobrine.component.core;

import com.ampznetwork.herobrine.component.config.model.Config;
import com.ampznetwork.herobrine.component.log.error.model.ErrorLogSender;
import com.ampznetwork.herobrine.util.ApplicationContextProvider;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.comroid.util.JdaUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.logging.Level;

@Log
@Component
public class DiscordProvider implements net.dv8tion.jda.api.hooks.EventListener, ErrorLogSender {
    @Lazy @Autowired JDA                        jda;
    /** this field exists to control lifecycle */
    @Autowired       ApplicationContextProvider context;
    @Autowired       ApplicationEventPublisher  publisher;

    @Bean
    public JDA jda(@Autowired Config config) throws InterruptedException {
        return JDABuilder.create(config.getDiscord().getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .addEventListeners(this)
                .build()
                .awaitReady();
    }

    @Override
    public void onEvent(@NonNull GenericEvent event) {
        try {
            publisher.publishEvent(event);
        } catch (Throwable t) {
            var guild = JdaUtil.getGuild(event).orElseGet(() -> jda.getGuildById(495506209881849856L));

            newErrorEntry().guild(guild).level(Level.SEVERE).message(t.getMessage()).throwable(t).queue();
        }
    }
}
