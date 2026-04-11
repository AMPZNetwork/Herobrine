package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.user.tags.model.UserTag;
import com.ampznetwork.herobrine.component.user.tags.model.UserTagProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface UserTagProviderRepository extends CrudRepository<UserTagProvider, UserTagProvider.Key> {
    Collection<UserTagProvider> findAllByGuildId(long guildId);

    @Query("""
            select utp.tag from UserTagProvider utp where utp.guildId = :guildId
             and (:selectedMethod is null or :selectedMethod = utp.method)
             and (:metaId is null or :metaId = utp.meta)""")
    Collection<UserTag> findContextualUserTags(long guildId, UserTagProvider.@Nullable Method selectedMethod, @Nullable Long metaId);

    @Query("""
            select utp.method from UserTagProvider utp where utp.guildId = :guildId
             and (:selectedTag is null or :selectedTag = utp.tag)
             and (:metaId is null or :metaId = utp.meta)""")
    Collection<UserTagProvider.Method> findContextualUserTagMethods(long guildId, @Nullable UserTag selectedTag, @Nullable Long metaId);
}
