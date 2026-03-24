package com.ampznetwork.herobrine.feature.chatmod;

import com.ampznetwork.herobrine.feature.chatmod.model.LoadedBridge;
import com.ampznetwork.herobrine.feature.chatmod.model.PlayerListEvent;
import com.ampznetwork.herobrine.feature.chatmod.model.ServerAwarePlayerList;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Service
@Command("playerlist")
public class ChannelPlayerListService extends ListenerAdapter {
    private final Map<LoadedBridge, ServerAwarePlayerList> playerLists = new ConcurrentHashMap<>();

    @Autowired JDA jda;

    @EventListener
    public void on(PlayerListEvent event) {
        var playerList = playerLists.computeIfAbsent(event.getBridge(), bridge -> {
            var config  = bridge.getConfig();
            var guild   = Objects.requireNonNull(jda.getGuildById(config.getGuildId()), "guild not found");
            var channel = Objects.requireNonNull(jda.getTextChannelById(config.getChannelId()), "channel not found");
            return new ServerAwarePlayerList(guild, channel);
        });

        playerList.poll(event);
        playerList.getChannel().getManager().setTopic("**__Online Players__**" + this).queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
