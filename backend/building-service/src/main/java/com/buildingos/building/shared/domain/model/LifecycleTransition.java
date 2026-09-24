package com.buildingos.building.shared.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Audited state change of an application or building (AP-04, D-13). {@code fromStatus} is null on creation. */
public record LifecycleTransition(UUID id, EntityType entityType, UUID entityId, String fromStatus, String toStatus,
        UUID actorUserId, String reason, Instant occurredAt) {
    public LifecycleTransition {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(toStatus, "toStatus");
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public static LifecycleTransition of(EntityType type, UUID entityId, Enum<?> from, Enum<?> to, UUID actor,
            String reason, Instant at) {
        return new LifecycleTransition(UUID.randomUUID(), type, entityId, from == null ? null : from.name(), to.name(),
                actor, reason, at);
    }
}
