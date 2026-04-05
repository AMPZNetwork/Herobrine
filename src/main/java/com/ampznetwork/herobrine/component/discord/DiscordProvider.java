package com.ampznetwork.herobrine.component.discord;

import com.ampznetwork.herobrine.component.config.model.Config;
import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogSender;
import com.ampznetwork.herobrine.util.ApplicationContextProvider;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.comroid.api.io.FileFlag;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.discord.JdaCommandAdapter;
import org.comroid.util.JdaUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.logging.Level;

@Log
@Component
public class DiscordProvider implements net.dv8tion.jda.api.hooks.EventListener, ErrorLogSender {
    public static final File COMMAND_PURGE_FILE = new File("./.purge_commands");

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

    @Bean
    @ConditionalOnBean({ CommandManager.class, JDA.class })
    public JdaCommandAdapter cmdrJdaAdapter(@Autowired CommandManager cmdr, @Autowired JDA jda) {
        var adp = new JdaCommandAdapter(cmdr, jda);
        adp.setPurgeCommands(FileFlag.consume(COMMAND_PURGE_FILE));
        cmdr.addChild(adp);
        return adp;
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
