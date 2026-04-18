package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.team.user.AbsenceInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface AbsenceInfoRepository extends CrudRepository<AbsenceInfo, AbsenceInfo.Key> {
    Collection<AbsenceInfo> findAllByGuildIdAndUserId(long guildId, long userId);
}
