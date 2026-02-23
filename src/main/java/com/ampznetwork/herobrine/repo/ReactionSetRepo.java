package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.reactionroles.model.ReactionRoleSet;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface ReactionSetRepo extends CrudRepository<ReactionRoleSet, ReactionRoleSet.Key> {
    Collection<ReactionRoleSet> findAllByGuildId(long guildId);

    Optional<ReactionRoleSet> findByMessageId(long messageId);

    @Modifying
    @Transactional
    @Query("delete ReactionRoleSet rrs where rrs.guildId = :idLong and rrs.name = :name")
    void removeBy(long idLong, String name);

    @Modifying
    @Transactional
    @Query("update ReactionRoleSet rrs set rrs.messageId = :messageId where rrs.guildId = :guildId and rrs.name = :name")
    void setMessageId(long guildId, String name, long messageId);
}
