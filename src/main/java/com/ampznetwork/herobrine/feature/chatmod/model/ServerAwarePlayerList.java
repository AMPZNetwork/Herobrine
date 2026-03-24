package com.ampznetwork.herobrine.feature.chatmod.model;

import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.comroid.api.func.Clearable;
import org.comroid.api.text.Markdown;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Value
public class ServerAwarePlayerList implements Clearable {
    Guild                         guild;
    TextChannel                   channel;
    Map<String, ServerPlayerList> servers = new ConcurrentHashMap<>();

    public void poll(PlayerListEvent event) {
        servers.computeIfAbsent(event.getPacket().getSource(), ServerPlayerList::new).poll(event);
    }

    @Override
    public String toString() {
        return servers.entrySet()
                .stream()
                .map(entry -> Markdown.Bold.apply(entry.getKey()) + '\n' + entry.getValue())
                .collect(Collectors.joining("\n- ", "- ", ""));
    }

    @Override
    public void clear() {
        servers.values().forEach(Clearable::clear);
    }

    public void refresh() {
        channel.getManager().setTopic("**__Online Players__**" + this).queue();
    }
}
