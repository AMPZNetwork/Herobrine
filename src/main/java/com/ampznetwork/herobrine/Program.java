package com.ampznetwork.herobrine;

import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import com.ampznetwork.herobrine.config.model.Config;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.comroid.annotations.Default;
import org.comroid.annotations.Description;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.io.FileFlag;
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
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log
@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.ampznetwork.herobrine.repo")
public class Program extends ListenerAdapter {
    private static final File COMMAND_PURGE_FILE = new File("./.purge_commands");

    public static void main(String[] args) {
        SpringApplication.run(Program.class, args);
    }

    @Command(permission = "8")
    @Description("Shutdown the Bot")
    public static String shutdown(
            @Command.Arg(value = "purgecommands",
                         required = false) @Description("Whether to purge commands on restart") @Nullable Boolean purgeCommands
    ) {
        if (Boolean.TRUE.equals(purgeCommands)) FileFlag.enable(COMMAND_PURGE_FILE);
        System.exit(0);
        return "Goodbye";
    }

    @Command
    @Description("Send a HTTP request")
    public static CompletableFuture<String> request(
            @Command.Arg @Description("URI to send a request to") String uri,
            @Command.Arg(required = false) @Default("GET") @Description("HTTP method to use; defaults to GET") REST.Method method,
            @Command.Arg(required = false) @Default("") @Description("Request body to use; default is empty") String body
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

    @Override
    public void onGenericEvent(GenericEvent event) {
        super.onGenericEvent(event);
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
    public ChatMessagePacket.ByteConverter packetConverter(@Autowired ObjectMapper objectMapper) {
        return new ChatMessagePacket.JacksonByteConverter(objectMapper);
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
    public JDA jda(@Autowired Config config) throws InterruptedException {
        return JDABuilder.create(config.getDiscord().getToken(), GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                .addEventListeners(this)
                .build()
                .awaitReady();
    }

    @Bean
    public JdaCommandAdapter cmdrJdaAdapter(@Autowired CommandManager cmdr, @Autowired JDA jda) {
        var adp = new JdaCommandAdapter(cmdr, jda);
        adp.setPurgeCommands(FileFlag.consume(COMMAND_PURGE_FILE));
        cmdr.addChild(adp);
        return adp;
    }

    @Order
    @EventListener
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).initialize();

        log.info("Initialized");
    }
}
