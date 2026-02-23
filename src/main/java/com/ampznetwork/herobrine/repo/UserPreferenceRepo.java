package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.model.UserPreferences;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;

@Repository
public interface UserPreferenceRepo extends CrudRepository<UserPreferences, @NotNull Long> {
    default void setTimezone(long userId, ZoneId zone) {
        var result = findById(userId);

        UserPreferences user;
        if (result.isPresent()) {
            user = result.get();
            user.setTimezone(zone);
        } else user = UserPreferences.builder().userId(userId).timezone(zone).build();

        save(user);
    }
}
