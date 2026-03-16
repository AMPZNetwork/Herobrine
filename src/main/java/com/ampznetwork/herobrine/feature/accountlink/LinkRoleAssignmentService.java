package com.ampznetwork.herobrine.feature.accountlink;

import com.ampznetwork.herobrine.feature.accountlink.model.LinkType;
import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkRoleConfiguration;
import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkedAccount;
import com.ampznetwork.herobrine.feature.accountlink.model.event.AccountLinkEvent;
import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogSender;
import com.ampznetwork.herobrine.repo.LinkRoleConfigRepository;
import com.ampznetwork.herobrine.repo.LinkedAccountRepository;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Log
@Service
@Command("link_role")
public class LinkRoleAssignmentService extends ListenerAdapter implements AuditLogSender {
    @Autowired JDA                      jda;
    @Autowired LinkRoleConfigRepository linkRoleConfigs;
    @Autowired LinkedAccountRepository  linkedAccounts;

    @Command(permission = "268435456")
    @Description("Refresh linked roles")
    public Object refresh(Guild guild) {
        var config = linkRoleConfigs.findById(guild.getIdLong()).orElse(null);
        if (config == null) return "No link roles are configured";

        return CompletableFuture.supplyAsync(() -> {

            var memberIds = guild.loadMembers().get().stream().map(ISnowflake::getIdLong).toList();
            var mappings = Streams.of(linkedAccounts.findAllById(memberIds))
                    .map(account -> new AccountMemberMapping(account, guild.getMemberById(account.getDiscordId())))
                    .toList();

            var actions = new HashSet<ActionTaken>();

            for (var type : LinkType.values()) {
                var roleId = type.getRoleId(config);
                if (roleId == 0) continue;

                var role = jda.getRoleById(roleId);
                if (role == null) continue;

                for (var mapping : mappings) {
                    if (type.getLinkedId(mapping.account) == null) continue;

                    var member = mapping.member;
                    var flag   = 0;

                    // has role?
                    if (member.getUnsortedRoles().contains(role)) flag |= 1;
                    // needs role?
                    if (checkUserForRole(guild, member, type)) flag |= 2;

                    switch (flag) {
                        case 0, 3:
                            // 0 = doesnt have, doesnt need
                            // 3 = does have, does need

                            // no action required
                            actions.add(new ActionTaken(mapping, role, Action.Unchanged));
                            break;
                        case 1:
                            // does have, doesnt need
                            // remove role from member

                            try {
                                guild.removeRoleFromMember(member, role).queue();
                                actions.add(new ActionTaken(mapping, role, Action.Removed));
                            } catch (Throwable t) {
                                actions.removeIf(action -> action.mapping.member.equals(mapping.member));
                                actions.add(new ActionTaken(mapping, role, Action.Failed, t));
                            }
                            break;
                        case 2:
                            // doesnt have, does need
                            // assign role

                            try {
                                guild.addRoleToMember(member, role).queue();
                                actions.add(new ActionTaken(mapping, role, Action.Assigned));
                            } catch (Throwable t) {
                                actions.removeIf(action -> action.mapping.member.equals(mapping.member));
                                actions.add(new ActionTaken(mapping, role, Action.Failed, t));
                            }
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }
            }

            var actionTree = new HashMap<Role, Map<Action, List<ActionTaken>>>();
            for (var action : actions)
                actionTree.computeIfAbsent(action.role, $ -> new HashMap<>())
                        .computeIfAbsent(action.action, $ -> new ArrayList<>())
                        .add(action);

            var message = new MessageCreateBuilder().useComponentsV2().addComponents(TextDisplay.of("# Actions taken"));

            for (var perRole : actionTree.entrySet()) {
                var role          = perRole.getKey();
                var roleTextBlock = new ArrayList<ContainerChildComponent>();

                roleTextBlock.add(TextDisplay.of("## Role %s".formatted(role)));

                for (var perAction : perRole.getValue()
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparingInt(entry -> entry.getKey().ordinal()))
                        .toList()) {
                    var actionTextBlock = new ArrayList<ContainerChildComponent>();

                    actionTextBlock.add(TextDisplay.of("### %s Members".formatted(perAction.getKey().name())));

                    for (var action : perAction.getValue()) {
                        actionTextBlock.add(action.throwable == null
                                            ? TextDisplay.of("- %s".formatted(action.mapping.member))
                                            : TextDisplay.of("- %s (reason: %s)".formatted(action.mapping.member,
                                                    action.throwable.getClass().getSimpleName())));
                    }

                    roleTextBlock.addAll(actionTextBlock);
                }

                var container = Container.of(roleTextBlock);

                message.addComponents(container.withAccentColor(role.getColors().getPrimary()));
            }

            return message.build();
        });
    }

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
                .message("%s was assigned role %s, but they don'throwable have their %s account linked".formatted(user,
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

    private enum Action {
        Assigned, Removed, Unchanged, Failed
    }

    private record AccountMemberMapping(LinkedAccount account, Member member) {}

    private record ActionTaken(AccountMemberMapping mapping, Role role, Action action, @Nullable Throwable throwable) {
        private ActionTaken(AccountMemberMapping mapping, Role role, Action action) {
            this(mapping, role, action, null);
        }
    }
}
