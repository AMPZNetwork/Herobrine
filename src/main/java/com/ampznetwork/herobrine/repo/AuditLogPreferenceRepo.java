package com.ampznetwork.herobrine.repo;

import com.ampznetwork.herobrine.feature.auditlog.model.AuditLogPreferences;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogPreferenceRepo extends CrudRepository<AuditLogPreferences, @NotNull Long> {}
