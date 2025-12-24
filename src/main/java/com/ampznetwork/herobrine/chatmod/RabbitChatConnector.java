package com.ampznetwork.herobrine.chatmod;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.ampznetwork.chatmod.api.model.config.ChatModules;
import com.ampznetwork.chatmod.api.model.config.channel.Channel;
import com.ampznetwork.chatmod.api.model.config.discord.DiscordChannel;
import com.ampznetwork.chatmod.api.model.protocol.ChatMessage;
import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import com.ampznetwork.chatmod.api.model.protocol.internal.ChatMessagePacketImpl;
import com.ampznetwork.chatmod.api.model.protocol.internal.PacketType;
import com.ampznetwork.chatmod.api.util.ChatMessageParser;
import com.ampznetwork.herobrine.model.cfg.Config;
import com.ampznetwork.libmod.api.util.Util;
import lombok.Value;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Debug;
import org.comroid.api.net.Rabbit;
import org.comroid.api.text.StringMode;
import org.comroid.api.tree.UncheckedCloseable;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.comroid.commands.model.CommandPrivacyLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.event.ClickEvent.*;
import static net.kyori.adventure.text.event.HoverEvent.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Log
@Value
@Component
public class RabbitChatConnector {
    public static final String ENDPOINT_NAME = "discord";
    @Autowired          Config config;

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        //event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        var channelBindings = event.getApplicationContext().getBean(ChannelBindings.class);
        log.info("Loaded %d channel bindings".formatted(channelBindings.size()));
    }

    @Bean
    public Rabbit rabbit(@Autowired Config config) {
        return Rabbit.of(config.getRabbitmq().getUri()).assertion();
    }

    @Bean
    public Rabbit.Exchange exchange(@Autowired Rabbit rabbit) {
        return rabbit.exchange("minecraft", "topic");
    }

    @Bean
    public ChannelBindings channelBindings(
            @Autowired ChatMessagePacket.ByteConverter packetConverter, @Autowired Config config,
            @Autowired Rabbit.Exchange exchange, @Autowired JDA jda
    ) {
        return new ChannelBindings(config.getChannels().stream().filter(channel -> {
            var discord = channel.getDiscord();
            return discord != null && jda.getTextChannelById(discord.getChannelId()) != null;
        }).collect(Collectors.toMap(Function.identity(), channel -> {
            var key            = "chat." + channel.getName();
            var route          = exchange.route("herobrine." + key, key, packetConverter);
            var discordChannel = jda.getTextChannelById(channel.getDiscord().getChannelId());
            assert discordChannel != null : "unreachable";
            return new ChannelRoute(channel, discordChannel, route);
        })));
    }

    @Command(privacy = CommandPrivacyLevel.EPHEMERAL)
    @Description("Shout into a channel")
    public void shout(
            Guild guild, User user,
            @Command.Arg(autoFillProvider = HerobrineChannelNames.class) @Description("Channel to shout into") String channel,
            @Command.Arg(stringMode = StringMode.GREEDY) @Description("Message to shout") String text
    ) {
        var channelRoute = bean(ChannelBindings.class).entrySet()
                .stream()
                .filter(e -> e.getKey().getName().equalsIgnoreCase(channel))
                .map(Map.Entry::getValue)
                .findAny()
                .orElseThrow(() -> new CommandError("No such channel: " + channel));
        var messageComponent = channelRoute.convertDiscordMessageToComponent(guild,
                user,
                null,
                channelRoute.config,
                text);
        var senderName = Optional.ofNullable(guild.getMember(user))
                .map(Member::getEffectiveName)
                .orElseGet(user::getName);
        var chatMessage = new ChatMessage(null, senderName, text, messageComponent.build());
        channelRoute.getRoute().send(new ChatMessagePacketImpl(PacketType.CHAT, ENDPOINT_NAME, channel, chatMessage));
    }

    @Value
    public static class ChannelRoute extends ListenerAdapter
            implements UncheckedCloseable, Consumer<ChatMessagePacket> {
        Channel                                  config;
        TextChannel                              channel;
        Rabbit.Exchange.Route<ChatMessagePacket> route;

        public ChannelRoute(Channel config, TextChannel channel, Rabbit.Exchange.Route<ChatMessagePacket> route) {
            this.config = config;
            (this.channel = channel).getJDA().addEventListener(this);
            (this.route = route).subscribeData(this);
        }

        private TextComponent.@NotNull Builder convertDiscordMessageToComponent(
                Guild guild, User author, @Nullable Message msg, Channel config, String contentRaw) {
            var inviteUrl = Optional.ofNullable(config.getDiscord()).map(DiscordChannel::getInviteUrl).orElse(null);
            var discord   = text("DISCORD", BLUE);
            if (inviteUrl != null) discord = discord.hoverEvent(showText(text("Get Invite...")))
                    .clickEvent(openUrl(inviteUrl));

            var authorColor = Optional.ofNullable(guild.getMember(author))
                    .stream()
                    .flatMap(member -> member.getRoles().stream())
                    .filter(role -> !role.getColors().isDefault())
                    .findFirst()
                    .map(role -> role.getColors().getPrimary())
                    .map(color -> TextColor.color(color.getRGB()))
                    .orElse(WHITE);
            var authorName = text(author.getEffectiveName(), authorColor);
            if (msg != null) authorName = authorName.hoverEvent(showText(text("Jump to Message...")))
                    .clickEvent(openUrl(msg.getJumpUrl()));
            else authorName = authorName.hoverEvent(showText(text("Message was shouted; cannot jump", RED)));

            return text().append(text("[", GRAY))
                    .append(discord)
                    .append(text("] #", GRAY))
                    .append(LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(config.getBestName())
                            .hoverEvent(showText(text("Open Channel...")))
                            .clickEvent(openUrl(channel.getJumpUrl())))
                    .append(text(" <", GRAY))
                    .append(authorName)
                    .append(text("> ", GRAY))
                    .append(new ChatMessageParser().parse(contentRaw));
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (!event.getChannel().equals(this.channel)) return;

            var author = event.getAuthor();
            if (author.isBot()) return;
            var msg   = event.getMessage();
            var guild = event.getGuild();

            var messageComponent = convertDiscordMessageToComponent(guild, author, msg, config, msg.getContentRaw());
            var message = new ChatMessage(null,
                    Optional.ofNullable(guild.getMember(author))
                            .map(Member::getEffectiveName)
                            .orElseGet(author::getName),
                    msg.getContentDisplay(),
                    messageComponent.build());

            var packet = new ChatMessagePacketImpl(PacketType.CHAT,
                    ENDPOINT_NAME,
                    config.getName(),
                    message,
                    List.of(ENDPOINT_NAME));
            route.send(packet);
        }

        @Override
        public void accept(ChatMessagePacket packet) {
            if (packet.getRoute().contains(ENDPOINT_NAME)) return;

            var builder = new WebhookMessageBuilder();
            var msg     = packet.getMessage();
            switch (packet.getPacketType()) {
                case CHAT:
                    var sb = new StringBuilder();
                    Util.Kyori.COMPONENT_TO_MARKDOWN.flatten(msg.getText(), sb::append);
                    builder.setContent(sb.toString());
                    break;
                case JOIN:
                    builder.setContent("> Joined the game");
                    break;
                case LEAVE:
                    builder.setContent("> Left the game");
                    break;
                default:
                    builder.setContent(msg.getMessageString());
                    break;
            }

            var senderName = msg.getSenderName();
            var message = builder.setUsername("[" + Util.Kyori.sanitizePlain(packet.getSource()) + "] " + senderName)
                    .setAvatarUrl("https://mc-heads.net/avatar/" + senderName)
                    .build();
            obtainWebhook().thenCompose(webhook -> webhook.send(message))
                    .exceptionally(Debug.exceptionLogger("Could not send message"));
        }

        @Override
        public void close() {
            channel.getJDA().removeEventListener(this);
            route.close();
        }

        private CompletableFuture<WebhookClient> obtainWebhook() {
            //noinspection DataFlowIssue -> we want the exception here for .exceptionallyCompose()
            return CompletableFuture.supplyAsync(() -> WebhookClient.withUrl(config.getDiscord().getWebhookUrl()))
                    .exceptionallyCompose(ignored -> channel.retrieveWebhooks()
                            .submit()
                            .thenCompose(webhooks -> webhooks.stream()
                                    .filter(webhook -> ChatModules.DiscordProviderConfig.WEBHOOK_NAME.equals(webhook.getName()))
                                    .findAny()
                                    .map(CompletableFuture::completedFuture)
                                    .orElseGet(() -> channel.createWebhook(ChatModules.DiscordProviderConfig.WEBHOOK_NAME)
                                            .submit()))
                            .thenApply(webhook -> WebhookClientBuilder.fromJDA(webhook).build()))
                    .exceptionally(Debug.exceptionLogger("Internal Exception when obtaining Webhook"));
        }
    }

    public static final class ChannelBindings extends ConcurrentHashMap<Channel, ChannelRoute>
            implements UncheckedCloseable {
        public ChannelBindings(Map<? extends Channel, ? extends ChannelRoute> m) {
            super(m);
        }

        @Override
        public void close() {
            for (var channelRoute : values())
                channelRoute.close();
        }
    }
}
