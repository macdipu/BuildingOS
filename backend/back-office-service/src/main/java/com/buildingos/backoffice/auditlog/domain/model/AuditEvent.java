package com.buildingos.backoffice.auditlog.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One audit row in the common InternalAuditEvent shape. {@code actorUserId} null = system; {@code buildingId},
 * {@code reason} and {@code role} are optional ({@code role} is auth-service only).
 */
public record AuditEvent(UUID id, AuditSource source, Instant occurredAt, UUID actorUserId, String action,
        String entityType, String entityId, UUID buildingId, String reason, String role) {
    public AuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(entityId, "entityId");
    }
}
