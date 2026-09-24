package com.buildingos.building.shared.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable evidence of a building-scoped privileged write (UO-10); before/after hold permitted fields only. */
public record AuditEntry(UUID id, UUID buildingId, UUID actorUserId, String action, String entityType, UUID entityId,
        String reason, Map<String, String> before, Map<String, String> after, Instant occurredAt) {
    public AuditEntry {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt");
        before = Map.copyOf(before);
        after = Map.copyOf(after);
    }

    public static AuditEntry of(UUID buildingId, UUID actor, String action, String entityType, UUID entityId,
            String reason, Map<String, String> before, Map<String, String> after, Instant at) {
        return new AuditEntry(UUID.randomUUID(), buildingId, actor, action, entityType, entityId, reason, before, after,
                at);
    }
}
