package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.team.user.TeamMemberInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface TeamMemberInfoRepository extends CrudRepository<TeamMemberInfo, TeamMemberInfo.Key> {
    Collection<TeamMemberInfo> findAllByGuildId(long guildId);
}
