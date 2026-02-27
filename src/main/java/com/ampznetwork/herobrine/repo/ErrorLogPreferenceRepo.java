package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.errorlog.model.ErrorLogPreferences;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorLogPreferenceRepo extends CrudRepository<ErrorLogPreferences, @NotNull Long> {}
