package com.buildingos.backoffice.shared.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Audited status change of a back-office entity (AU-01, BOC-06). {@code fromStatus} is null on creation;
 * {@code actorUserId} is null for a system transition (automatic expiry).
 */
public record LifecycleTransition(UUID id, EntityType entityType, UUID entityId, String fromStatus, String toStatus,
        UUID actorUserId, String reason, Instant occurredAt) {
    public LifecycleTransition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(toStatus, "toStatus");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static LifecycleTransition of(EntityType type, UUID entityId, Enum<?> from, Enum<?> to, UUID actor,
            String reason, Instant at) {
        return new LifecycleTransition(UUID.randomUUID(), type, entityId, from == null ? null : from.name(), to.name(),
                actor, reason, at);
    }

    public boolean isSystem() { return actorUserId == null; }
}
