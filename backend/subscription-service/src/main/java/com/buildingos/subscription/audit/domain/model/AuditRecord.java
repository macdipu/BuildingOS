package com.buildingos.subscription.audit.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Read view of an {@code audit_event} row (F6-T5c); before/after payloads are deliberately absent. */
public record AuditRecord(UUID id, UUID actorUserId, String action, String entityType, String entityId,
        Instant occurredAt) {
}
