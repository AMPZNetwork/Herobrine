package com.ampznetwork.herobrine.config;

import com.ampznetwork.herobrine.Program;
import com.ampznetwork.herobrine.config.model.Config;
import com.ampznetwork.herobrine.util.ApplicationContextProvider;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.api.config.ConfigurationManager;
import org.comroid.api.func.util.Debug;
import org.comroid.api.java.ResourceLoader;
import org.comroid.commands.impl.CommandManager;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

@Log
@Service
public class ConfigurationService extends ListenerAdapter {
    @Lazy @Autowired TextChannel discordConfigChannel;

    @Bean
    public File configFile(@Autowired File botDir) throws IOException {
        var file = new File(botDir, "config.json5");
        ResourceLoader.assertFile(Program.class, "/config.json5", file, null);
        return file;
    }

    @Bean
    public ConfigurationManager<Config> configurationManager(
            @Autowired ApplicationContextProvider context,
            @Autowired File configFile
    ) {
        var manager = new ConfigurationManager<>(context, Config.class, configFile.getAbsolutePath());
        manager.initialize();
        return manager;
    }

    @Bean
    public Config config(
            @Autowired ConfigurationManager<Config> configurationManager
    ) {
        return configurationManager.getConfig();
    }

    @Bean
    public TextChannel discordConfigChannel(@Autowired JDA jda) {
        return jda.getTextChannelById(Debug.isDebug() ? 1466884271430963251L : 1466884558719946945L);
    }

    @Bean
    public ConfigurationManager<Config>.Presentation$JDA configurationPresentation$discord(
            @Autowired TextChannel discordConfigChannel, @Autowired ConfigurationManager<Config> configurationManager) {
        var presentation = configurationManager.new Presentation$JDA(discordConfigChannel);

        try {
            presentation.clear();
        } catch (Throwable t) {
            log.log(Level.WARNING, "Could not clear old config presentation messages", t);
        }

        presentation.refresh();
        return presentation;
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        if (!event.getChannel().equals(discordConfigChannel)) return;
        if (event.getAuthor() instanceof SelfUser) return;
        event.getMessage().delete().queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
