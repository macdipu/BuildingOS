package com.buildingos.auth.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One effective platform-role grant or revoke (D-37). */
public record PlatformRoleAuditEntry(
        UUID id, UUID actorUserId, UUID targetUserId, PlatformRole role, PlatformRoleAuditAction action,
        Instant occurredAt) {
    public PlatformRoleAuditEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(targetUserId, "targetUserId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
