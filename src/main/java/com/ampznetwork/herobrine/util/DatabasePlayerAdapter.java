package com.ampznetwork.herobrine.util;

import com.ampznetwork.herobrine.repo.PlayerRepo;
import com.ampznetwork.libmod.api.interop.game.PlayerIdentifierAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class DatabasePlayerAdapter implements PlayerIdentifierAdapter {
    @Autowired PlayerRepo players;

    @Override
    public Optional<com.ampznetwork.libmod.api.entity.Player> getPlayer(UUID uuid) {
        return players.findById(uuid).map(com.ampznetwork.herobrine.model.entity.Player::upgrade);
    }

    @Override
    public Optional<com.ampznetwork.libmod.api.entity.Player> getPlayer(String name) {
        return players.findPlayerByName(name).map(com.ampznetwork.herobrine.model.entity.Player::upgrade);
    }
}
