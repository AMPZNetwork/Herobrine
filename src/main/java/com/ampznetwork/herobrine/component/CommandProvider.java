package com.ampznetwork.herobrine.component;

import lombok.extern.java.Log;
import org.comroid.commands.impl.CommandManager;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Log
@Component
public class CommandProvider {
    @Bean
    public CommandManager cmdr(
    ) {
        var cmdr = new CommandManager();
        cmdr.addChild(this);
        cmdr.register(this);
        return cmdr;
    }

    @Order
    @EventListener
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).initialize();

        log.info("Initialized");
    }
}
