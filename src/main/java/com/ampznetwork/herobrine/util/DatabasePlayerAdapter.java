package com.ampznetwork.herobrine.util;

import com.ampznetwork.chatmod.api.model.Player;
import com.ampznetwork.chatmod.api.model.PlayerIdentifierAdapter;
import com.ampznetwork.herobrine.model.CachedPlayer;
import com.ampznetwork.herobrine.repo.PlayerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DatabasePlayerAdapter implements PlayerIdentifierAdapter {
    @Autowired PlayerRepo players;

    @Override
    public Optional<Player> getPlayer(UUID uuid) {
        return players.findById(uuid).map(CachedPlayer::upgrade);
    }

    @Override
    public Optional<Player> getPlayer(String name) {
        return players.findPlayerByName(name).map(CachedPlayer::upgrade);
    }
}
