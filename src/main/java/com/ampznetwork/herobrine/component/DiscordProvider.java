package com.ampznetwork.herobrine.component;

import com.ampznetwork.herobrine.component.config.model.Config;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.comroid.api.func.util.Event;
import org.comroid.api.io.FileFlag;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.discord.JdaCommandAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;

@Log
@Component
public class DiscordProvider extends ListenerAdapter {
    public static final File COMMAND_PURGE_FILE = new File("./.purge_commands");

    @Lazy @Autowired Event.Bus<GenericEvent> jdaEventBus;

    @Override
    public void onGenericEvent(@NotNull GenericEvent event) {
        jdaEventBus.accept(event);
    }

    @Bean
    public JDA jda(@Autowired Config config) throws InterruptedException {
        return JDABuilder.create(config.getDiscord().getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .build()
                .awaitReady();
    }

    @Bean
    public Event.Bus<GenericEvent> jdaEventBus() {
        return new Event.Bus<>();
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
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
