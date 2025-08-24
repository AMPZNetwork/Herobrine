package com.ampznetwork.herobrine;

import com.ampznetwork.herobrine.model.cfg.Config;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.comroid.api.config.ConfigurationManager;
import org.comroid.api.func.ext.Context;
import org.comroid.api.io.FileFlag;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.discord.JdaCommandAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Driver;

@SpringBootApplication
public class Program {
    private static final File COMMAND_PURGE_FILE = new File("./.purge_commands");

    public static void main(String[] args) {
        SpringApplication.run(Program.class, args);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper(JsonFactory.builder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .build()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_YAML_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
                .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
    }

    @Bean
    public File botDir() {
        return new File("/srv/herobrine/");
    }

    @Bean
    public File configFile(@Autowired File botDir) {
        return new File(botDir, "config.json");
    }

    @Bean
    public ConfigurationManager<Config> configManager(
            @Autowired Context context,
            @Autowired File configFile
    ) {
        return new ConfigurationManager<>(context, Config.class, configFile.getAbsolutePath());
    }

    @Bean
    public Config config(@Autowired ConfigurationManager<Config> configManager) {
        return configManager.initialize();
    }

    @Bean
    public DataSource database(@Autowired Config config) {
        return DataSourceBuilder.create()
                .driverClassName(Driver.class.getCanonicalName())
                .url(config.getDatabase().getUri())
                .username(config.getDatabase().getUsername())
                .password(config.getDatabase().getPassword())
                .build();
    }

    @Bean
    public JDA jda(@Autowired Config config) {
        return JDABuilder.create(config.getDiscord().getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .build();
    }

    @Bean
    public CommandManager cmdr() {
        var cmdr = new CommandManager();
        cmdr.addChild(this);

        cmdr.register(this);

        return cmdr;
    }

    @Bean
    public JdaCommandAdapter cmdrJdaAdapter(@Autowired CommandManager cmdr, @Autowired JDA jda)
    throws InterruptedException {
        try {
            var adp = new JdaCommandAdapter(cmdr, jda.awaitReady());
            adp.setPurgeCommands(FileFlag.consume(COMMAND_PURGE_FILE));
            return adp;
        } finally {
            cmdr.initialize();
        }
    }
}
