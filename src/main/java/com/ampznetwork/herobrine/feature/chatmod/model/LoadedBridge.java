package com.ampznetwork.herobrine.feature.chatmod.model;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.ampznetwork.chatmod.api.model.config.ChatModules;
import com.ampznetwork.chatmod.api.model.protocol.ChatMessage;
import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import com.ampznetwork.chatmod.api.model.protocol.PacketType;
import com.ampznetwork.chatmod.api.parse.ChatMessageParser;
import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkedAccount;
import com.ampznetwork.herobrine.feature.chatmod.ChannelBridgeService;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import com.ampznetwork.herobrine.util.ApplicationContextProvider;
import com.ampznetwork.libmod.api.entity.Player;
import com.ampznetwork.libmod.api.util.Util;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.java.Log;
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
import org.comroid.api.func.util.Streams;
import org.comroid.api.net.Rabbit;
import org.comroid.api.text.Markdown;
import org.comroid.api.tree.UncheckedCloseable;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.event.ClickEvent.*;
import static net.kyori.adventure.text.event.HoverEvent.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Log
@Value
@EqualsAndHashCode(of = { "config", "channel" })
public class LoadedBridge implements UncheckedCloseable {
    ApplicationEventPublisher publisher;
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

        switch (packet.getPacketType()) {
            case CHAT:
                var sb = new StringBuilder();
                Util.Kyori.COMPONENT_TO_MARKDOWN.flatten(msg.getText(), sb::append);
                var string = sb.toString();
                string = convertMentions_MinecraftToDiscord(string);
                builder.setContent(string);
                break;
            case JOIN:
                builder.setContent("> Joined the game");
                publisher.publishEvent(new PlayerListEvent(this, packet, PlayerListEvent.Type.JOIN));
                break;
            case LEAVE:
                builder.setContent("> Left the game");
                publisher.publishEvent(new PlayerListEvent(this, packet, PlayerListEvent.Type.LEAVE));
                break;
            default:
                builder.setContent(PlainTextComponentSerializer.plainText().serialize(packet.getMessage().getFullText()));
                break;
        }

        var senderName = msg.getSenderName();
        var message = builder.setUsername("[" + Util.Kyori.sanitizePlain(packet.getSource()) + "] " + senderName)
                .setAvatarUrl("https://mc-heads.net/avatar/" + senderName)
                .build();
        obtainWebhook().thenCompose(webhook -> webhook.send(message)).exceptionally(Debug.exceptionLogger("Could not send message"));
    }

    public void handle(MessageReceivedEvent event) {
        if (!event.getChannel().equals(channel)) return;

        var author = event.getAuthor();
        if (author.isBot()) return;
        var msg     = event.getMessage();
        var guild   = event.getGuild();
        var content = convertMentions_DiscordToMinecraft(msg.getContentRaw());
        var bundle  = convertDiscordMessageToComponent(guild, author, msg, content);

        handle(guild, author, bundle);
    }

    public void handle(Guild guild, User author, String contentRaw) {
        contentRaw = convertMentions_DiscordToMinecraft(contentRaw);
        var bundle = convertDiscordMessageToComponent(guild, author, null, contentRaw);

        handle(guild, author, bundle);
    }

    public void handle(Guild guild, User author, ChatMessageParser.MessageBundle bundle) {
        var message = new ChatMessage(null, Optional.ofNullable(guild.getMember(author)).map(Member::getEffectiveName).orElseGet(author::getName), bundle);
        var packet = new ChatMessagePacket(PacketType.CHAT,
                ChannelBridgeService.ENDPOINT_NAME,
                config.getName(),
                message,
                List.of(ChannelBridgeService.ENDPOINT_NAME));

        route.send(packet);
    }

    private String convertMentions_DiscordToMinecraft(String string) {
        final var result = ApplicationContextProvider.wrap(LinkedAccountRepository.class);
        if (result.isEmpty()) return string;

        var matcher = Message.MentionType.USER.getPattern().matcher(string);
        var sb      = new StringBuilder();
        var links = Streams.of(result.get().findAll())
                .filter(account -> account.getMinecraftId() != null)
                .collect(Collectors.toMap(account -> String.valueOf(account.getDiscordId()), account -> Player.fetchUsername(account.getMinecraftId()).join()));

        while (matcher.find()) {
            var userId = matcher.group(1);
            var link   = links.get(userId);

            matcher.appendReplacement(sb, Markdown.Italic.apply('@' + link));
        }

        matcher.appendTail(sb);

        return sb.toString();
    }

    private String convertMentions_MinecraftToDiscord(String string) {
        final var result = ApplicationContextProvider.wrap(LinkedAccountRepository.class);
        if (result.isEmpty()) return string;

        var links = Streams.of(result.get().findAll())
                .filter(account -> account.getMinecraftId() != null)
                .collect(Collectors.toMap(account -> Player.fetchUsername(account.getMinecraftId()).join(), LinkedAccount::getDiscordId));

        for (var mapping : links.entrySet())
            string = string.replaceAll(mapping.getKey(), "<@%d>".formatted(mapping.getValue()));

        return string;
    }

    private ChatMessageParser.MessageBundle convertDiscordMessageToComponent(Guild guild, User author, @Nullable Message msg, String contentRaw) {
        var discord = text("DISCORD", BLUE);
        if (config.inviteUrl != null) discord = discord.hoverEvent(showText(text("Get Invite..."))).clickEvent(openUrl(config.inviteUrl));

        var authorColor = Optional.ofNullable(guild.getMember(author))
                .stream()
                .flatMap(member -> member.getRoles().stream())
                .filter(role -> !role.getColors().isDefault())
                .findFirst()
                .map(role -> role.getColors().getPrimary())
                .map(color -> TextColor.color(color.getRGB()))
                .orElse(WHITE);
        var authorName = text(author.getEffectiveName(), authorColor);
        if (msg != null) authorName = authorName.hoverEvent(showText(text("Jump to Message..."))).clickEvent(openUrl(msg.getJumpUrl()));
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
                        .orElseGet(() -> channel.createWebhook(ChatModules.DiscordProviderConfig.WEBHOOK_NAME).submit()))
                .thenApply(webhook -> WebhookClientBuilder.fromJDA(webhook).build());
    }
}
