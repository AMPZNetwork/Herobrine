package com.ampznetwork.herobrine;

import com.ampznetwork.herobrine.discord.DiscordBotProvider;
import com.ampznetwork.herobrine.model.cfg.Config;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import org.comroid.annotations.Default;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.io.FileFlag;
import org.comroid.api.java.ResourceLoader;
import org.comroid.api.net.REST;
import org.comroid.api.text.Markdown;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.impl.discord.JdaCommandAdapter;
import org.jetbrains.annotations.Nullable;
import org.mariadb.jdbc.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.ampznetwork.herobrine.repo")
public class Program {
    private static final File COMMAND_PURGE_FILE = new File("./.purge_commands");

    public static void main(String[] args) {
        SpringApplication.run(Program.class, args);
    }

    @Command(permission = "8")
    public static String shutdown(
            @Command.Arg(value = "purgecommands", required = false) @Nullable Boolean purgeCommands
    ) {
        if (Boolean.TRUE.equals(purgeCommands)) FileFlag.enable(COMMAND_PURGE_FILE);
        System.exit(0);
        return "Goodbye";
    }

    @Command
    public static CompletableFuture<String> request(
            @Command.Arg String uri, @Command.Arg(required = false) @Default("GET") REST.Method method,
            @Command.Arg(required = false) @Default("") String body
    ) {
        return REST.request(method, uri, body == null ? null : JSON.Parser.parse(body))
                .execute()
                .thenApply(response -> {
                    var headersText = response.getHeaders()
                            .entrySet()
                            .stream()
                            .map(e -> e.getKey() + ": " + String.join("; ", e.getValue()))
                            .collect(Collectors.joining("\n"));
                    var data = response.getBody().toSerializedString();
                    return Markdown.CodeBlock.apply(headersText + "\n" + data);
                });
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
    public File configFile(@Autowired File botDir) throws IOException {
        var file = new File(botDir, "config.json5");
        ResourceLoader.assertFile(Program.class, "/config.json5", file, null);
        return file;
    }

    @Bean
    public Config config(@Autowired ObjectMapper objectMapper, @Autowired File configFile) throws IOException {
        return objectMapper.readValue(configFile, Config.class);
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
    public CommandManager cmdr(
    ) {
        var cmdr = new CommandManager();
        cmdr.addChild(this);
        cmdr.register(this);

        return cmdr;
    }

    @Bean
    @ConditionalOnBean(DiscordBotProvider.class)
    public JDA jda(@Autowired DiscordBotProvider provider) {
        return provider.getDefaultModule().getJda();
    }

    @Bean
    @ConditionalOnBean(DiscordBotProvider.class)
    public JdaCommandAdapter cmdrJdaAdapter(@Autowired CommandManager cmdr, @Autowired JDA jda)
    throws InterruptedException {
        var adp = new JdaCommandAdapter(cmdr, jda.awaitReady());
        adp.setPurgeCommands(FileFlag.consume(COMMAND_PURGE_FILE));
        cmdr.addChild(adp);
        return adp;
    }

    @Order@EventListener
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).initialize();

        log.info("Initialized");
    }
}
