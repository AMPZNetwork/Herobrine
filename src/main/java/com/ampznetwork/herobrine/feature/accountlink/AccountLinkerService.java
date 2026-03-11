package com.ampznetwork.herobrine.feature.accountlink;

import com.ampznetwork.chatmod.api.model.protocol.ChatMessage;
import com.ampznetwork.chatmod.api.model.protocol.ChatMessagePacket;
import com.ampznetwork.chatmod.api.model.protocol.PacketType;
import com.ampznetwork.chatmod.lite.model.abstr.ChatModConfig;
import com.ampznetwork.herobrine.feature.accountlink.model.LinkedAccount;
import com.ampznetwork.herobrine.feature.chatmod.ChannelBridgeService;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import com.ampznetwork.libmod.api.entity.Player;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.comroid.annotations.Description;
import org.comroid.api.net.Token;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.comroid.commands.model.CommandError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

@Log
@Service
@Command("link")
public class AccountLinkerService extends ListenerAdapter {
    private final Collection<PendingLink> pending = new HashSet<>();
    @Autowired LinkedAccountRepository linkedAccounts;
    @Autowired ChannelBridgeService    minecraftChannelBridgeService;

    @Command
    @Description("Verify account linkage after requesting a token")
    public CompletableFuture<String> verify(
            UserSnowflake user,
            @Command.Arg @Description("The token you received") String token
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var result = pending.stream()
                    .filter(link -> link.userId() == user.getIdLong() && link.token().equals(token))
                    .findAny();

            if (result.isEmpty()) return "Sorry, wrong token.";
            var pending = result.get();

            LinkedAccount account;
            var           accountResult = linkedAccounts.findById(user.getIdLong());
            if (accountResult.isPresent()) account = accountResult.get();
            else //noinspection SwitchStatementWithTooFewBranches
                switch (pending) {
                    case PendingMinecraftLink mc -> {
                        var playerId = Player.fetchId(mc.minecraftUsername).join();
                        account = new LinkedAccount(user.getIdLong(), playerId);
                    }
                    default -> throw new CommandError("Internal error\n-# Please contact the bot developers");
                }

            linkedAccounts.save(account);
            this.pending.remove(pending);
            return "Your accounts have successfully been linked!";
        });
    }

    @Command
    @Description("Request a token to link your Minecraft account")
    public CompletableFuture<String> minecraft(
            Guild guild, UserSnowflake user,
            @Command.Arg @Description("Minecraft Username to be linked") String username
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var result = linkedAccounts.findById(user.getIdLong()).or(() -> {
                var minecraftId = Player.fetchId(username).join();
                return linkedAccounts.findByMinecraftId(minecraftId);
            });

            if (result.isPresent())
                return "Your account is already linked to Minecraft username " + Player.fetchUsername(result.get()
                        .getMinecraftId()).join();

            var token = newToken();
            var systemChannel = minecraftChannelBridgeService.getSystemChannel(guild.getIdLong());

            if (systemChannel == null) return "No system channel was found for this server";
            systemChannel.send(createMinecraftTokenPacket(user, username, token));

            var pendingLink = new PendingMinecraftLink(user.getIdLong(), username, token);
            pending.add(pendingLink);
            return "Please check in-game for your verification token, then use `/link verify <token>` to verify your account linkage";
        });
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private ChatMessagePacket createMinecraftTokenPacket(UserSnowflake user, String username, String token) {
        var text = Component.text("Please verify your Minecraft account using this token: ")
                .append(Component.text(token, NamedTextColor.GOLD));
        var message = new ChatMessage(null, "Herobrine", text);

        return ChatMessagePacket.builder()
                .packetType(PacketType.CHAT)
                .source("herobrine")
                .channel(ChatModConfig.SYSTEM_CHANNEL_NAME)
                .message(message)
                .recipient(username)
                .build();
    }

    private String newToken() {
        String token;

        do {
            token = Token.random(6, false);
        } while (pending.stream().map(PendingLink::token).anyMatch(token::equals));

        return token;
    }

    private interface PendingLink {
        long userId();

        String token();
    }

    private record PendingMinecraftLink(long userId, String minecraftUsername, String token) implements PendingLink {}
}
