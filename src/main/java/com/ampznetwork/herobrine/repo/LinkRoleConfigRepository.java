package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.accountlink.model.entity.LinkRoleConfiguration;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LinkRoleConfigRepository extends CrudRepository<LinkRoleConfiguration, @NotNull Long> {
    @Query("select lrc from LinkRoleConfiguration lrc where lrc.minecraftRoleId = :roleId or lrc.hytaleRoleId = :roleId")
    Optional<LinkRoleConfiguration> findByAnyRoleId(long roleId);
}
