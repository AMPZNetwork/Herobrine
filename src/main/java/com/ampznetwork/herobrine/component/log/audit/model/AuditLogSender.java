package com.ampznetwork.herobrine.component.log.audit.model;

import com.ampznetwork.herobrine.component.log.audit.AuditLogService;

import static com.ampznetwork.herobrine.util.ApplicationContextProvider.*;

public interface AuditLogSender {
    default String getAuditSourceName() {
        return getClass().getSimpleName();
    }

    default AuditLogService audit() {
        return bean(AuditLogService.class);
    }

    default AuditLogService.EntryAPI newAuditEntry() {
        return audit().newEntry().source(this);
    }
}
