package com.ampznetwork.herobrine.feature.auditlog.model;

import com.ampznetwork.herobrine.feature.auditlog.AuditLogService;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

public interface AuditLogSender {
    default String getAuditSourceName() {
        return getClass().getSimpleName();
    }

    default AuditLogService audit() {
        return bean(AuditLogService.class);
    }
}
