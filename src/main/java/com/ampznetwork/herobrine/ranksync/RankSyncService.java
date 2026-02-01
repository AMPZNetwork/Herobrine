package com.ampznetwork.herobrine.ranksync;

import com.ampznetwork.herobrine.config.model.Config;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import org.comroid.api.func.util.Debug;
import org.comroid.api.model.Authentication;
import org.comroid.api.net.luckperms.LuckPermsApiWrapper;
import org.comroid.api.net.luckperms.component.GroupsApi;
import org.comroid.api.net.luckperms.component.UserApi;
import org.comroid.api.net.luckperms.model.group.GroupData;
import org.comroid.api.net.luckperms.model.node.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log
@Service
@EnableScheduling
@NoArgsConstructor
public class RankSyncService {
    public static final String META_DISCORD_USER_ID = "ranksync-discord-user-id";
    public static final String META_DISCORD_ROLE_ID = "ranksync-discord-role-id";

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

    @Scheduled(initialDelay = 5_000, fixedRate = 600_000)
    @SuppressWarnings({ "SuspiciousToArrayCall", "RedundantSuppression" /* false-positive */ })
    public void rankUpdate() {
        final var repo = new ConcurrentHashMap<UUID, UserEntry>() {
            UserEntry compute(UUID id) {
                return computeIfAbsent(id, UserEntry::new);
            }
        };

        final var userApi  = lpApi.child(UserApi.class).assertion();
        final var groupApi = lpApi.child(GroupsApi.class).assertion();

        groupApi.getIDs()
                .thenCompose(names -> {
                    final @SuppressWarnings("unchecked") CompletableFuture<GroupData>[] requests = new CompletableFuture[names.size()];
                    var                                                                 i        = 0;
                    for (var name : names) requests[i] = groupApi.get(name);
                    return CompletableFuture.allOf(requests)
                            .thenApply($ -> Arrays.stream(requests)
                                    .map(CompletableFuture::join)
                                    .peek(group -> log.fine("Fetched generic group data: " + group))
                                    .collect(Collectors.toMap(GroupData::name, Function.identity())));
                })
                .thenCompose(groups -> groupApi.search()
                        .metaKey(META_DISCORD_ROLE_ID)
                        .execute()
                        .thenApply(results -> results.stream()
                                .peek(group -> log.fine("Fetched LP group by role id metadata: " + group))
                                .flatMap(result -> result.results()
                                        .stream()
                                        .sorted(Comparator.comparingInt(node -> groups.containsKey(result.name())
                                                                                ? groups.get(result.name()).weight()
                                                                                : 0))
                                        .map(Node::getKeyValue)
                                        .flatMap(roleId -> Stream.ofNullable(jda.getRoleById(roleId)))
                                        .map(role -> new AbstractMap.SimpleImmutableEntry<>(groups.get(result.name()),
                                                role)))
                                .collect(Collectors.toMap(e -> e.getKey().name(), Function.identity()))))
                .thenCompose(groups -> {
                    var fetchDiscordIds = userApi.search()
                            .metaKey(META_DISCORD_USER_ID)
                            .execute()
                            .thenAcceptAsync(results -> {
                                for (var result : results) {
                                    log.fine("Fetched LP user by user id metadata: " + result);
                                    result.results()
                                            .stream()
                                            .filter(Node::value)
                                            .findAny()
                                            .map(Node::getKeyValue)
                                            .map(Long::parseLong)
                                            .ifPresent(repo.compute(result.uniqueId())::setDiscordId);
                                }
                            });
                    var fetchDiscordGroups = userApi.search()
                            .nodeKeyStartsWith("group")
                            .execute()
                            .thenAcceptAsync(results -> {
                                for (var result : results) {
                                    result.results()
                                            .stream()
                                            .map(Node::getKeyValue)
                                            .flatMap(groupName -> groups.containsKey(groupName)
                                                                  ? Stream.empty()
                                                                  : Stream.ofNullable(groups.get(groupName)))
                                            .max(Comparator.comparingInt(e -> e.getKey().weight()))
                                            .ifPresent(e -> {
                                                log.fine("Top group & role found for user %s: group %s, role %s".formatted(
                                                        result.uniqueId(),
                                                        e.getKey(),
                                                        e.getValue()));
                                                repo.compute(result.uniqueId())
                                                        .setGroup(e.getKey())
                                                        .setRole(e.getValue());
                                            });
                                }
                            });
                    return CompletableFuture.allOf(fetchDiscordIds, fetchDiscordGroups);
                })
                .thenComposeAsync($ -> CompletableFuture.allOf(repo.values().stream().flatMap(entry -> {
                    var role  = entry.getRole();
                    var guild = role.getGuild();

                    var user = jda.getUserById(entry.getDiscordId());
                    if (user == null) return Stream.empty();

                    var member = guild.getMember(user);
                    if (member == null || member.getRoles().contains(role)) return Stream.empty();

                    log.fine("Applying role %s to member %s".formatted(role, member));
                    return Stream.of(guild.addRoleToMember(member, role).submit());
                }).toArray(CompletableFuture[]::new)))
                .exceptionally(Debug.exceptionLogger("Could not fetch all necessary LuckPerms data"));
    }
}
