package com.ampznetwork.herobrine.config;

import com.ampznetwork.herobrine.Program;
import com.ampznetwork.herobrine.config.model.Config;
import com.ampznetwork.herobrine.util.ApplicationContextProvider;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.comroid.api.config.ConfigurationManager;
import org.comroid.api.func.util.Debug;
import org.comroid.api.java.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class ConfigurationService {
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
        presentation.refresh();
        return presentation;
    }
}
