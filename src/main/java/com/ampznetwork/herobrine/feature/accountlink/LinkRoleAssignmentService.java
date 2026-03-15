package com.ampznetwork.herobrine.feature.accountlink;

import com.ampznetwork.herobrine.feature.accountlink.model.LinkType;
import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkRoleConfiguration;
import com.ampznetwork.herobrine.feature.accountlink.model.event.AccountLinkEvent;
import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.repo.LinkRoleConfigRepository;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.logging.Level;

@Log
@Service
@Command("link_role")
public class LinkRoleAssignmentService extends ListenerAdapter implements AuditLogSender {
    @Autowired JDA                      jda;
    @Autowired LinkRoleConfigRepository linkRoleConfigs;
    @Autowired LinkedAccountRepository  linkedAccounts;

    @Command(permission = "268435456")
    @Description("Configure the role to be set for users with a linked Minecraft account")
    public String configure_minecraft(
            Guild guild, @Command.Arg(required = false) @Description({
                    "The role to be assigned", "Set nothing to remove the role"
            }) @Nullable Role role
    ) {
        var config = linkRoleConfigs.findById(guild.getIdLong()).orElse(null);

        if (role == null) {
            if (config == null) return "Nothing was configured; no action applied";

            config.setMinecraftRoleId(0);
        } else {
            if (config == null) config = LinkRoleConfiguration.builder()
                    .guildId(guild.getIdLong())
                    .minecraftRoleId(role.getIdLong())
                    .build();
            else config.setMinecraftRoleId(role.getIdLong());
        }

        linkRoleConfigs.save(config);
        return "Configuration updated";
    }

    @Override
    public void onGuildMemberRoleAdd(@NonNull GuildMemberRoleAddEvent event) {
        if (event.getRoles().size() != 1) return;
        var role = event.getRoles().getFirst();

        var result = linkRoleConfigs.findByAnyRoleId(role.getIdLong());
        if (result.isEmpty()) return;
        var config = result.get();

        var guild = event.getGuild();
        var user  = event.getUser();
        var type  = config.getTypeOfRole(role.getIdLong());

        if (type == null) return;
        if (checkUserForRole(guild, user, type)) return;

        newAuditEntry().guild(guild)
                .level(Level.WARNING)
                .message("%s was assigned role %s, but they don't have their %s account linked".formatted(user,
                        role,
                        type))
                .queue();
        guild.removeRoleFromMember(user, role)
                .reason("User does not have their %s account linked".formatted(type))
                .queue();
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    @EventListener
    public void on(AccountLinkEvent event) {
        var guild = event.getGuild();
        var type  = event.getType();

        var config = linkRoleConfigs.findById(guild.getIdLong()).orElseThrow();
        var userId = event.getAccount().getDiscordId();
        var user   = Objects.requireNonNull(jda.getUserById(userId), "guild member " + userId);

        if (!checkUserForRole(guild, user, type)) return;

        var roleId = type.getRoleId(config);
        if (roleId == 0) return;

        var role = Objects.requireNonNull(jda.getRoleById(roleId), "link role " + roleId);

        newAuditEntry().guild(guild).level(Level.INFO).message("Assigning role %s to %s".formatted(role, user)).queue();
        guild.addRoleToMember(user, role).queue();
    }

    private boolean checkUserForRole(Guild guild, UserSnowflake user, LinkType type) {
        var config = linkRoleConfigs.findById(guild.getIdLong()).orElse(null);

        var roleId = type.getRoleId(config);
        if (roleId == 0) return false;

        var role = jda.getRoleById(roleId);
        if (role == null) return false;

        var account = linkedAccounts.findById(user.getIdLong()).orElse(null);
        if (account == null) return false;

        return type.getLinkedId(account) != null;
    }
}
