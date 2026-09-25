package com.buildingos.subscription.audit.application.listauditevents;

import java.time.Instant;
import java.util.UUID;

/** Common internal audit read item (F6-T5c); before/after payloads are never exposed. */
public record AuditEvent(UUID id, String source, Instant occurredAt, UUID actorUserId, String action,
        String entityType, String entityId, UUID buildingId, String reason) {
}
