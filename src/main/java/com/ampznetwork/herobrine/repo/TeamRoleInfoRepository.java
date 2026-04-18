package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.team.role.TeamRoleInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface TeamRoleInfoRepository extends CrudRepository<TeamRoleInfo, TeamRoleInfo.Key> {
    Collection<TeamRoleInfo> findAllByGuildId(long guildId);
}
