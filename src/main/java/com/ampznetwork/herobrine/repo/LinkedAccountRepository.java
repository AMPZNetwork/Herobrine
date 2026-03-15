package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkedAccount;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LinkedAccountRepository extends CrudRepository<LinkedAccount, @NotNull Long> {
    Optional<LinkedAccount> findByMinecraftId(UUID minecraftId);
}
