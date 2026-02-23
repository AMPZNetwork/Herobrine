package com.ampznetwork.herobrine.component.web;

import lombok.extern.java.Log;
import org.comroid.annotations.Default;
import org.comroid.annotations.Description;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.net.REST;
import org.comroid.api.text.Markdown;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log
@Component
public class InteractiveRequestService {
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

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        //event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
