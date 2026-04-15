package com.ampznetwork.herobrine.component.web;

import com.ampznetwork.herobrine.component.core.MaintenanceProvider;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.User;
import org.comroid.annotations.Default;
import org.comroid.annotations.Description;
import org.comroid.api.data.seri.adp.JSON;
import org.comroid.api.net.REST;
import org.comroid.api.text.Markdown;
import org.comroid.interaction.annotation.Interaction;
import org.comroid.interaction.annotation.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Log
@Component
public class InteractiveRequestService {
    @Autowired MaintenanceProvider maintenance;

    @Interaction
    @Description("Send a HTTP request")
    public CompletableFuture<String> request(
            User user,
            @Parameter @Description("URI to send a request to") String uri,
            @Parameter(required = false) @Default("GET") @Description("HTTP method to use; defaults to GET") REST.Method method,
            @Parameter(required = false) @Default("") @Description("Request body to use; default is empty") String body
    ) {
        maintenance.verifySuperadmin(user);

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
}
