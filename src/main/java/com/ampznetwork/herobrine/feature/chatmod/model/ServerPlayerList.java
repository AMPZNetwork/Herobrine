package com.ampznetwork.herobrine.feature.chatmod.model;

import lombok.Value;
import org.comroid.api.func.Clearable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Value
public class ServerPlayerList implements Clearable {
    String            serverName;
    Map<UUID, String> players = new ConcurrentHashMap<>();

    public void poll(PlayerListEvent event) {
        var player = event.getPacket().getMessage().getSender();
        if (player == null) return;
        var playerId = player.getUuid();
        var flag     = 0;

        // inList
        flag |= players.containsKey(playerId) ? 1 : 0;
        // joining
        flag |= event.getType() == PlayerListEvent.Type.JOIN ? 2 : 0;

        switch (flag) {
            case 1:
                // remove from list
                players.remove(playerId);
                break;
            case 2:
                // add to list
                players.put(playerId, player.getName());
                break;
        }
    }

    @Override
    public String toString() {
        return players.values().stream().collect(Collectors.joining("\n- ", "- ", ""));
    }

    @Override
    public void clear() {
        players.clear();
    }
}
