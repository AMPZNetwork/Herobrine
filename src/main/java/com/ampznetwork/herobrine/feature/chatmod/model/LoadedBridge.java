package com.ampznetwork.herobrine.feature.chatmod.model;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.ampznetwork.chatmod.api.model.config.ChatModules;
import com.ampznetwork.chatmod.api.model.protocol.ChatMessage;
import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import com.ampznetwork.chatmod.api.model.protocol.PacketType;
import com.ampznetwork.chatmod.api.parse.ChatMessageParser;
import com.ampznetwork.herobrine.feature.chatmod.ChannelBridgeService;
import com.ampznetwork.libmod.api.util.Util;
import lombok.Value;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.comroid.api.func.util.Debug;
import org.comroid.api.net.Rabbit;
import org.comroid.api.tree.UncheckedCloseable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.event.ClickEvent.*;
import static net.kyori.adventure.text.event.HoverEvent.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Value
public class LoadedBridge implements UncheckedCloseable {
    ChannelBridgeService                     bridgeService;
    ChannelBridgeConfig                      config;
    TextChannel                              channel;
    Rabbit.Exchange.Route<ChatMessagePacket> route;

    @Override
    public void close() {
        route.close();
    }

    public void handle(ChatMessagePacket packet) {
        if (packet.getRoute().contains(ChannelBridgeService.ENDPOINT_NAME)) return;

        var builder = new WebhookMessageBuilder();
        var msg     = packet.getMessage();
        var sender  = packet.getMessage().getSender();

        // todo: playerlist - bridgeService.touch(sender);

        switch (packet.getPacketType()) {
            case CHAT:
                var sb = new StringBuilder();
                Util.Kyori.COMPONENT_TO_MARKDOWN.flatten(msg.getText(), sb::append);
                builder.setContent(sb.toString());
                break;
            case JOIN:
                builder.setContent("> Joined the game");
                if (sender == null) break;
                // todo: playerlist - bridgeService.playerJoin(packet);
                break;
            case LEAVE:
                builder.setContent("> Left the game");
                if (sender == null) break;
                // todo: playerlist - bridgeService.playerLeave(packet);
                break;
            default:
                builder.setContent(PlainTextComponentSerializer.plainText()
                        .serialize(packet.getMessage().getFullText()));
                break;
        }

        var senderName = msg.getSenderName();
        var message = builder.setUsername("[" + Util.Kyori.sanitizePlain(packet.getSource()) + "] " + senderName)
                .setAvatarUrl("https://mc-heads.net/avatar/" + senderName)
                .build();
        obtainWebhook().thenCompose(webhook -> webhook.send(message))
                .exceptionally(Debug.exceptionLogger("Could not send message"));
    }

    public void handle(MessageReceivedEvent event) {
        if (!event.getChannel().equals(channel)) return;

        var author = event.getAuthor();
        if (author.isBot()) return;
        var msg    = event.getMessage();
        var guild  = event.getGuild();
        var bundle = convertDiscordMessageToComponent(guild, author, msg, msg.getContentRaw());

        handle(guild, author, bundle);
    }

    public void handle(Guild guild, User author, String contentRaw) {
        var bundle = convertDiscordMessageToComponent(guild, author, null, contentRaw);

        handle(guild, author, bundle);
    }

    public void handle(Guild guild, User author, ChatMessageParser.MessageBundle bundle) {
        var message = new ChatMessage(null,
                Optional.ofNullable(guild.getMember(author)).map(Member::getEffectiveName).orElseGet(author::getName),
                bundle);
        var packet = new ChatMessagePacket(PacketType.CHAT,
                ChannelBridgeService.ENDPOINT_NAME,
                config.getName(),
                message,
                List.of(ChannelBridgeService.ENDPOINT_NAME));

        route.send(packet);
    }

    private ChatMessageParser.MessageBundle convertDiscordMessageToComponent(
            Guild guild, User author, @Nullable Message msg, String contentRaw) {
        var discord = text("DISCORD", BLUE);
        if (config.inviteUrl != null) discord = discord.hoverEvent(showText(text("Get Invite...")))
                .clickEvent(openUrl(config.inviteUrl));

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

        var bundle = com.ampznetwork.chatmod.api.parse.ChatMessageParser.parse(contentRaw,
                ChannelBridgeService.CHAT_MOD_CONFIG,
                config.toChannel(),
                null /* todo: map player by discord user */,
                author.getEffectiveName());

        if (msg != null) {
            var size = msg.getAttachments().size();
            if (size != 0) bundle = bundle.withSuffix(text(" [%d attachment%s]".formatted(size, size == 1 ? "" : "s")));
        }

        return bundle.withPrefix(text().append(text("[", GRAY))
                .append(discord)
                .append(text("] #", GRAY))
                .append(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(config.getBestName())
                        .hoverEvent(showText(text("Open Channel...")))
                        .clickEvent(openUrl(channel.getJumpUrl())))
                .append(text(" <", GRAY))
                .append(authorName)
                .append(text("> ", GRAY))
                .build());
    }

    private CompletableFuture<WebhookClient> obtainWebhook() {
        return channel.retrieveWebhooks()
                .submit()
                .thenCompose(webhooks -> webhooks.stream()
                        .filter(webhook -> ChatModules.DiscordProviderConfig.WEBHOOK_NAME.equals(webhook.getName()))
                        .findAny()
                        .map(CompletableFuture::completedFuture)
                        .orElseGet(() -> channel.createWebhook(ChatModules.DiscordProviderConfig.WEBHOOK_NAME)
                                .submit()))
                .thenApply(webhook -> WebhookClientBuilder.fromJDA(webhook).build());
    }
}
