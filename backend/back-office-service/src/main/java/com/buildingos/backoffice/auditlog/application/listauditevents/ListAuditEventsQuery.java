package com.buildingos.backoffice.auditlog.application.listauditevents;

import com.buildingos.backoffice.auditlog.domain.model.AuditSource;
import java.time.Instant;
import java.util.UUID;

/** All optional: null {@code source} = every source, null {@code limit} = default. */
public record ListAuditEventsQuery(Instant since, Instant until, AuditSource source, String entityType,
        UUID actorUserId, Integer limit) {
}
