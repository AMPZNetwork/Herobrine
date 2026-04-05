package com.ampznetwork.herobrine.component;

import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import org.comroid.interaction.InteractionCore;
import org.comroid.interaction.adapter.jda.JdaAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Log
@Component
public class CommandProvider {
    @Bean
    public InteractionCore core() {
        return new InteractionCore();
    }

    @Bean
    @ConditionalOnBean(JDA.class)
    public JdaAdapter jdaAdapter(@Autowired InteractionCore core, @Autowired JDA jda) {
        var adapter = new JdaAdapter(core, jda);
        core.addChild(adapter);
        return adapter;
    }

    @Order
    @EventListener
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(InteractionCore.class).initialize();

        log.info("Initialized");
    }
}
