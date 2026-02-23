package com.ampznetwork.herobrine.feature.ranksync;

import com.ampznetwork.herobrine.component.config.model.Config;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.comroid.annotations.Description;
import org.comroid.api.func.exc.ThrowingFunction;
import org.comroid.api.func.util.Debug;
import org.comroid.api.func.util.Pair;
import org.comroid.api.func.util.Streams;
import org.comroid.api.model.Authentication;
import org.comroid.api.net.luckperms.LuckPermsApiWrapper;
import org.comroid.api.net.luckperms.component.GroupsApi;
import org.comroid.api.net.luckperms.component.UserApi;
import org.comroid.api.net.luckperms.model.group.GroupData;
import org.comroid.api.net.luckperms.model.group.GroupSearchResult;
import org.comroid.api.net.luckperms.model.node.Node;
import org.comroid.api.net.luckperms.model.node.NodeType;
import org.comroid.api.net.luckperms.model.user.UserData;
import org.comroid.commands.Command;
import org.comroid.commands.impl.CommandManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log
@Service
@NoArgsConstructor
public class RankSyncService extends ListenerAdapter {
    public static final String META_DISCORD_USER_ID   = "ranksync-discord-user-id";
    public static final String META_DISCORD_ROLE_ID   = "ranksync-discord-role-id";
    public static final String META_DISCORD_ROLE_TIER = "ranksync-tier";

    @Lazy @Autowired LuckPermsApiWrapper lpApi;
    @Lazy @Autowired JDA                 jda;

    @Bean
    public LuckPermsApiWrapper lpApi(@Autowired Config config) {
        var cfg = config.getLuckperms();
        return LuckPermsApiWrapper.builder()
                .baseUrl(cfg.getUri())
                .credentials(Authentication.ofToken(cfg.getToken()))
                .build();
    }

    @Command(value = "rankupdate", permission = "8")
    @Description("Perform a refresh on LuckPerms ranks")
    @Scheduled(initialDelay = 1, fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    @SuppressWarnings({ "SuspiciousToArrayCall", "RedundantSuppression" /* false-positive */ })
    public CompletableFuture<String> rankUpdate() {
        final var userApi  = lpApi.child(UserApi.class).assertion();
        final var groupApi = lpApi.child(GroupsApi.class).assertion();
        final var actions = new int[3];

        return groupApi.getIDs()
                .thenCompose(this::parallelFetchGroups)
                .thenCompose(groups -> {
                    final var tiers = groups.values()
                            .stream()
                            .filter(group -> group.metadata() != null && group.metadata().meta() != null)
                            .filter(group -> group.metadata().meta().containsKey(META_DISCORD_ROLE_ID))
                            .collect(Collectors.groupingBy(group -> group.metadata()
                                    .meta()
                                    .get(META_DISCORD_ROLE_TIER)));

                    return userApi.search()
                            .metaKey(META_DISCORD_USER_ID)
                            .execute()
                            .thenCompose(results -> {
                                final @SuppressWarnings("unchecked") CompletableFuture<UserData>[] requests = new CompletableFuture[results.size()];
                                var                                                                i        = 0;
                                for (var result : results) {
                                    requests[i] = userApi.get(result.uniqueId());
                                    requests[i].thenAcceptAsync(group -> log.fine("Fetched generic user data: " + group));
                                    i++;
                                }
                                return CompletableFuture.allOf(requests)
                                        .thenApply($ -> Arrays.stream(requests)
                                                .map(ThrowingFunction.sneaky(CompletableFuture::get))
                                                .toList());
                            })
                            .thenApply(users -> users.stream()
                                    .filter(user -> user.metadata() != null && user.metadata().meta() != null)
                                    .map(user -> {
                                        final var discordId = user.metadata().meta().get(META_DISCORD_USER_ID);
                                        final var userRoles = Arrays.stream(user.nodes())
                                                .filter(node -> node.type() == NodeType.inheritance)
                                                .map(Node::getKeyValue)
                                                .collect(Streams.expandRecursive(groupName -> groups.containsKey(
                                                        groupName)
                                                                                              ? Arrays.stream(groups.get(
                                                                groupName).nodes())
                                                                                                      .filter(node -> node.type() == NodeType.inheritance)
                                                                                                      .map(Node::getKeyValue)
                                                                                              : Stream.empty()))
                                                .sorted(Comparator.comparingInt(groupName -> groups.containsKey(
                                                        groupName)
                                                                                             ? groups.get(groupName)
                                                                                                     .weight()
                                                                                             : 0).reversed())
                                                .flatMap(groupName -> tiers.entrySet()
                                                        .stream()
                                                        .flatMap(e -> e.getValue()
                                                                .stream()
                                                                .filter(group -> group.name().equals(groupName))
                                                                .map(group -> new Pair<>(e.getKey(), group))))
                                                .flatMap(Streams.distinctBy(Pair::getFirst))
                                                .map(Pair::getSecond)
                                                .filter(group -> group.metadata() != null && group.metadata()
                                                                                                     .meta() != null)
                                                .filter(group -> group.metadata()
                                                        .meta()
                                                        .containsKey(META_DISCORD_ROLE_ID))
                                                .sorted(Comparator.comparingInt(GroupData::weight).reversed())
                                                .map(group -> group.metadata().meta().get(META_DISCORD_ROLE_ID))
                                                .flatMap(roleId -> Stream.ofNullable(jda.getRoleById(roleId)))
                                                .toList();
                                        return new Pair<>(discordId, userRoles);
                                    })
                                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
                })
                .thenCompose(results -> CompletableFuture.allOf(results.entrySet().stream().flatMap(entry -> {
                    var user = jda.getUserById(entry.getKey());
                    if (user == null) {
                        actions[2]++;
                        return Stream.empty();
                    }

                    return Stream.of(CompletableFuture.allOf(entry.getValue().stream().flatMap(role -> {
                        var guild  = role.getGuild();
                        var member = guild.getMember(user);

                        if (member == null) {
                            log.fine("Member %s not found in guild %s".formatted(user.getEffectiveName(),
                                    guild.getName()));
                            actions[2]++;
                            return Stream.empty();
                        }
                        if (member.getRoles().contains(role)) {
                            log.fine("Member %s already has role %s".formatted(member.getEffectiveName(),
                                    role.getName()));
                            actions[1]++;
                            return Stream.empty();
                        }

                        log.fine("Applying role %s to member %s".formatted(role.getName(), member.getEffectiveName()));
                        return Stream.of(guild.addRoleToMember(member, role)
                                .submit()
                                .thenRun(() -> actions[0]++)
                                .exceptionally(Debug.exceptionLogger("Could not add role %s to member %s".formatted(role.getName(),
                                        member.getEffectiveName()))));
                    }).toArray(CompletableFuture[]::new)));
                }).toArray(CompletableFuture[]::new)))
                .exceptionally(Debug.exceptionLogger("Could not fetch all necessary LuckPerms data"))
                .thenApply($ -> ("Rank Synchronization complete!\n%d actions performed, %d actions skipped, %d invalid actions").formatted(
                        actions[0],
                        actions[1],
                        actions[2]));
    }

    @Command(value = "rankinfo", permission = "8")
    @Description("Show synced ranks and their metadata")
    public CompletableFuture<EmbedBuilder> rankInfo() {
        final var groupApi = lpApi.child(GroupsApi.class).assertion();

        return groupApi.search()
                .metaKey(META_DISCORD_ROLE_ID)
                .execute()
                .thenApply(results -> results.stream().map(GroupSearchResult::name).toList())
                .thenCompose(this::parallelFetchGroups)
                .thenApply(groups -> {
                    final var embed = new EmbedBuilder();
                    final var tiers = groups.values()
                            .stream()
                            .filter(group -> group.metadata() != null && group.metadata().meta() != null)
                            .filter(group -> group.metadata().meta().containsKey(META_DISCORD_ROLE_ID))
                            .collect(Collectors.groupingBy(group -> group.metadata()
                                    .meta()
                                    .get(META_DISCORD_ROLE_TIER)));

                    tiers.entrySet()
                            .stream()
                            .sorted(Comparator.comparingInt(e -> e.getKey().matches("\\d+")
                                                                 ? Integer.parseInt(e.getKey())
                                                                 : 0))
                            .map(tier -> {
                                var content = tier.getValue()
                                        .stream()
                                        .filter(group -> group.metadata() != null && group.metadata().meta() != null)
                                        .filter(group -> group.metadata().meta().containsKey(META_DISCORD_ROLE_ID))
                                        .sorted(Comparator.comparingInt(GroupData::weight))
                                        .flatMap(group -> {
                                            var roleId = group.metadata().meta().get(META_DISCORD_ROLE_ID);
                                            var role   = jda.getRoleById(roleId);
                                            if (role == null) return Stream.empty();

                                            return Stream.of("Group `%s` -> Role %s".formatted(group.bestName(),
                                                    role.getAsMention()));
                                        })
                                        .collect(Collectors.joining("\n- ", "- ", ""));
                                return new MessageEmbed.Field("Tier `%s`".formatted(tier.getKey()), content, false);
                            })
                            .forEachOrdered(embed::addField);

                    return embed;
                });
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void on(ApplicationStartedEvent event) {
        event.getApplicationContext().getBean(JDA.class).addEventListener(this);
        event.getApplicationContext().getBean(CommandManager.class).register(this);

        log.info("Initialized");
    }

    private CompletionStage<Map<String, GroupData>> parallelFetchGroups(Collection<String> names) {
        final @SuppressWarnings("unchecked") CompletableFuture<GroupData>[] requests = new CompletableFuture[names.size()];
        final var                                                           groupApi = lpApi.child(GroupsApi.class)
                .assertion();

        var i = 0;
        for (var name : names) {
            requests[i] = groupApi.get(name);
            requests[i].thenAcceptAsync(group -> log.fine("Fetched generic group data: " + group));
            i++;
        }
        return CompletableFuture.allOf(requests)
                .thenApply($ -> Arrays.stream(requests)
                        .map(ThrowingFunction.sneaky(CompletableFuture::get))
                        .collect(Collectors.toMap(GroupData::name, Function.identity())));
    }
}
