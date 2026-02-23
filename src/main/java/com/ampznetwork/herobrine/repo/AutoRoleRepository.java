package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.autorole.model.AutoRoleMapping;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface AutoRoleRepository extends CrudRepository<AutoRoleMapping, AutoRoleMapping.Key> {
    Collection<AutoRoleMapping> findAllByGuildId(long guildId);
}
