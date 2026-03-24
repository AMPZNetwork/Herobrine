package com.ampznetwork.herobrine.feature.chatmod;

import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import com.ampznetwork.chatmod.lite.model.abstr.ChatModConfig;
import com.ampznetwork.herobrine.feature.chatmod.model.GuildChannelNameAutoFillProvider;
import com.ampznetwork.herobrine.feature.chatmod.model.LoadedBridge;
import com.ampznetwork.herobrine.feature.chatmod.protocol.JacksonPacketConverter;
import com.ampznetwork.herobrine.repo.ChannelBridgeConfigRepo;
import lombok.Getter;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.api.net.Rabbit;
import org.comroid.api.text.StringMode;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Service
@Command("chat")
public class ChannelBridgeService extends ListenerAdapter {
    public static final String        ENDPOINT_NAME   = "discord";
    public static final ChatModConfig CHAT_MOD_CONFIG = new ChatModConfig() {
        @Override
        public String getServerName() {
            return "§9DISCORD";
        }

        @Override
        public Rabbit getRabbit() {
            return null;
        }

        @Override
        public String getFormattingScheme() {
            return "&7[%server_name%&7] #&6%channel_name%&7 <%player_name%&7> &r%message%";
        }
    };

    @Autowired JDA                       jda;
    @Autowired ChannelBridgeConfigRepo   channelBridges;
    @Autowired JacksonPacketConverter    packetConverter;
    @Autowired ApplicationEventPublisher publisher;

    private final @Getter Collection<LoadedBridge>                                     loaded         = new HashSet<>();
    private final         Map<@NotNull Long, Rabbit.Exchange.Route<ChatMessagePacket>> systemChannels = new ConcurrentHashMap<>();

    public @Nullable Rabbit.Exchange.Route<ChatMessagePacket> getSystemChannel(long guildId) {
        return systemChannels.getOrDefault(guildId, null);
    }

    @Command(permission = "16")
    @Description("Reloads channel bridge configurations and listeners")
    public String reload(@Nullable Guild guild) {
        for (var bridge : loaded.toArray(LoadedBridge[]::new)) {
            if (guild != null && bridge.getConfig().getGuildId() != guild.getIdLong()) continue;
            bridge.close();
            loaded.remove(bridge);
        }

        // load channel bridges
        for (var config : guild == null ? channelBridges.findAll() : channelBridges.findAllByGuildId(guild.getIdLong())) {
            var key = "chat." + config.getChannelName();

            var channel = jda.getTextChannelById(config.getChannelId());
            if (channel == null) throw new CommandError("Could not find text channel by id: `%d`".formatted(config.getChannelId()));

            var route = Rabbit.of("ChatMod Channel Bridge", config.getRabbitUri())
                    .assertion("Could not instantiate Rabbit")
                    .exchange("minecraft", "topic")
                    .route("herobrine." + key, key, packetConverter);

            var bridge = new LoadedBridge(publisher, this, config, channel, route);

            route.subscribeData(bridge::handle);
            loaded.add(bridge);
        }

        // load system channels
        for (var each : (guild == null ? jda.getGuilds() : List.of(guild))) {
            var guildId = each.getIdLong();

            channelBridges.findRabbitByGuildId(guildId)
                    .stream()
                    .flatMap(uri -> Rabbit.of(uri).stream())
                    .map(rabbit -> rabbit.exchange("minecraft", "topic"))
                    .map(exchange -> exchange.route("herobrine.chat.system", "chat.system", packetConverter))
                    .forEach(route -> {
                        route.subscribeData(this::handleSystemPackets);

                        systemChannels.compute(guildId, (k, v) -> {
                            if (v != null) v.close();
                            return route;
                        });
                    });
        }

        return "Loaded %d channel bridges and %d system channels".formatted(loaded.size(), systemChannels.size());
    }

    @Command
    @Description("Shout a message into a specific channel")
    public void shout(
            Guild guild, User user,
            @Command.Arg(autoFillProvider = GuildChannelNameAutoFillProvider.class) @Description("The channel to shout into") String channel,
            @Command.Arg(stringMode = StringMode.GREEDY) @Description("The message to shout") String message
    ) {
        loaded.stream().filter(bridge -> bridge.getConfig().getChannelName().equals(channel)).forEach(bridge -> bridge.handle(guild, user, message));
    }

    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        var guild   = event.getGuild();
        var channel = event.getChannel();

        loaded.stream().filter(bridge -> {
            var config = bridge.getConfig();

            return config.getGuildId() == guild.getIdLong() && config.getChannelId() == channel.getIdLong();
        }).forEach(bridge -> bridge.handle(event));
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        reload(null);

        log.info("Initialized");
    }

    private void handleSystemPackets(ChatMessagePacket packet) {
        // maybe this will be useful later
    }
}
