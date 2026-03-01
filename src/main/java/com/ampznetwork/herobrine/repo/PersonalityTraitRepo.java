package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.personality.model.PersonalityTrait;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface PersonalityTraitRepo extends CrudRepository<PersonalityTrait, PersonalityTrait.Key> {
    Collection<PersonalityTrait> findAllByGuildId(long guildId);
}
