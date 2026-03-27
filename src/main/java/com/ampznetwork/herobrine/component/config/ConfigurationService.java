package com.ampznetwork.herobrine.component.config;

import com.ampznetwork.herobrine.Program;
import com.ampznetwork.herobrine.component.config.model.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.comroid.api.java.ResourceLoader;
import org.comroid.commands.impl.CommandManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Log
@Service
public class ConfigurationService {
    @Bean
    public File configFile(@Autowired File botDir) throws IOException {
        var file = new File(botDir, "config.json5");
        ResourceLoader.assertFile(Program.class, "/config.json5", file, null);
        return file;
    }

    @Bean
    public Config config(@Autowired File configFile, @Autowired ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(configFile, Config.class);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
