package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.model.CachedPlayer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepo extends CrudRepository<CachedPlayer, UUID> {
    Optional<CachedPlayer> findPlayerByName(String name);
}
