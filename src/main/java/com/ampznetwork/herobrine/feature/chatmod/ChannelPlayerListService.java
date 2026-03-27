package com.ampznetwork.herobrine.feature.chatmod;

import com.ampznetwork.herobrine.feature.chatmod.model.LoadedBridge;
import com.ampznetwork.herobrine.feature.chatmod.model.PlayerListEvent;
import com.ampznetwork.herobrine.feature.chatmod.model.ServerAwarePlayerList;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.comroid.annotations.Description;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(ChannelBridgeService.class)
public class ChannelPlayerListService {
    private final Map<LoadedBridge, ServerAwarePlayerList> playerLists = new ConcurrentHashMap<>();

    @Autowired JDA                  jda;
    @Autowired ChannelBridgeService bridgeService;

    @Command(permission = "8192")
    @Description("Clear player lists for this channel")
    public String clear(Guild guild, TextChannel channel) {
        var bridge = bridgeService.getLoaded().stream().filter(it -> {
            var config = it.getConfig();
            return config.getGuildId() == guild.getIdLong() && config.getChannelId() == channel.getIdLong();
        }).findAny().orElseThrow(() -> new CommandError("Not in a bridged channel"));

        if (!playerLists.containsKey(bridge)) return "No player list to clear";

        playerLists.remove(bridge).clear();

        return "Lists cleared";
    }

    @EventListener
    public void on(PlayerListEvent event) {
        var playerList = playerLists.computeIfAbsent(event.getBridge(), bridge -> {
            var config  = bridge.getConfig();
            var guild   = Objects.requireNonNull(jda.getGuildById(config.getGuildId()), "guild not found");
            var channel = Objects.requireNonNull(jda.getTextChannelById(config.getChannelId()), "channel not found");
            return new ServerAwarePlayerList(guild, channel);
        });

        playerList.poll(event);
        playerList.refresh();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
