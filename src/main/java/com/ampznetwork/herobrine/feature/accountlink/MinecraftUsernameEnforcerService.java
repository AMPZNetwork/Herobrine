package com.ampznetwork.herobrine.feature.accountlink;

import com.ampznetwork.herobrine.feature.accountlink.model.LinkedAccount;
import com.ampznetwork.herobrine.feature.accountlink.model.MinecraftUsernameEnforcerConfig;
import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogSender;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import com.ampznetwork.herobrine.repo.MinecraftUsernameEnforcerConfigRepository;
import com.ampznetwork.libmod.api.entity.Player;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.api.func.util.Streams;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Log
@Service
@Command("mc-username-enforcer")
public class MinecraftUsernameEnforcerService extends ListenerAdapter implements AuditLogSender, ErrorLogSender {
    @Autowired JDA                                       jda;
    @Autowired MinecraftUsernameEnforcerConfigRepository enforcerConfigRepo;
    @Autowired LinkedAccountRepository                   linkedAccounts;

    @Command(permission = "134217728")
    @Description("Change Minecraft username enforcer configuration")
    public String configure(
            Guild guild,
            @Command.Arg(autoFill = { "yes", "no" }) @Description("Whether to forcibly change nicknames") String enforce
    ) {
        var guildId = guild.getIdLong();
        var config  = enforcerConfigRepo.findById(guildId).orElse(null);
        var flag = "yes".equalsIgnoreCase(enforce);

        if (config == null) config = new MinecraftUsernameEnforcerConfig(guildId, flag);
        else config.setEnforceNicknames(flag);

        enforcerConfigRepo.save(config);

        return "Config updated";
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void updateAll() {
        update(null);
    }

    @Command(permission = "134217728")
    @Description("Update all nicknames in this guild")
    public void update(@Nullable Guild guild) {
        for (var config : guild == null
                          ? enforcerConfigRepo.findAll()
                          : enforcerConfigRepo.findById(guild.getIdLong()).stream().toList()) {
            if (!config.isEnforceNicknames()) continue;

            guild = jda.getGuildById(config.getGuildId());
            if (guild == null) {
                log.warning("Could not find guild by id " + config.getGuildId());
                continue;
            }

            var members = guild.loadMembers().get();
            var minecraftPlayernames = Streams.of(linkedAccounts.findAllById(members.stream()
                            .map(ISnowflake::getIdLong)
                            .toList()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(LinkedAccount::getDiscordId,
                            account -> Player.fetchUsername(account.getMinecraftId()).join()));

            record FailedUser(UserSnowflake user, String playerName, Throwable throwable) {}
            var failed = new HashSet<FailedUser>();
            for (var member : members) {
                if (!minecraftPlayernames.containsKey(member.getIdLong())) continue;

                var playerName = minecraftPlayernames.get(member.getIdLong());
                if (playerName == null) continue;

                var nickname = member.getNickname();
                if (nickname != null && !nickname.equals(playerName)) auditIllegalNickname(guild, member, playerName);

                try {
                    member.modifyNickname(playerName)
                            .reason(playerName == null
                                    ? "User does not have their account Linked"
                                    : "Name does not match Minecraft Username")
                            .queue();
                } catch (Throwable t) {
                    failed.add(new FailedUser(member, playerName, t));
                }
            }

            newErrorEntry().guild(guild)
                    .level(Level.WARNING)
                    .message("Unable to set nickname for users:\n- " + failed.stream()
                            .map(entry -> "%s - `%s` - Reason: %s".formatted(entry.user,
                                    entry.playerName,
                                    entry.throwable.getClass().getSimpleName()))
                            .collect(Collectors.joining("\n- ")))
                    .queue();
        }
    }

    private void auditIllegalNickname(Guild guild, Member member, String playerName) {
        newAuditEntry().guild(guild)
                .level(Level.WARNING)
                .message("Nickname of %s does not match required player name `%s`".formatted(member, playerName))
                .queue();
    }

    @Override
    public void onGuildMemberUpdateNickname(@NonNull GuildMemberUpdateNicknameEvent event) {
        var       guild  = event.getGuild();
        final var config = enforcerConfigRepo.findById(guild.getIdLong()).orElse(null);
        if (config == null || !config.isEnforceNicknames()) return;

        var member        = event.getMember();
        var accountResult = linkedAccounts.findById(member.getIdLong());

        if (accountResult.isEmpty()) {
            var oldNick = event.getOldNickname();

            auditIllegalNickname(guild, member, "<undefined>");
            member.modifyNickname(oldNick).queue();

            return;
        }

        var account           = accountResult.get();
        var minecraftUsername = Player.fetchUsername(account.getMinecraftId()).join();

        member.modifyNickname(minecraftUsername).queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }
}
