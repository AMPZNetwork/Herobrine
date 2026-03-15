package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.accountlink.model.entity.MinecraftUsernameEnforcerConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;

public interface MinecraftUsernameEnforcerConfigRepository
        extends CrudRepository<MinecraftUsernameEnforcerConfig, @NotNull Long> {}
