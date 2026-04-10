package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.component.user.tags.model.UserTagProvider;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface UserTagProviderRepository extends CrudRepository<UserTagProvider, UserTagProvider.Key> {
    Collection<UserTagProvider> findAllByGuildId(long guildId);
}
