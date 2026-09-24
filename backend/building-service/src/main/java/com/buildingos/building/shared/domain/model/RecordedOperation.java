package com.buildingos.building.shared.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Accepted idempotent request: actor/action/operationId → fingerprint and result, stored with the write. */
public record RecordedOperation(UUID actorUserId, String action, UUID operationId, UUID buildingId,
        String requestFingerprint, UUID resultEntityId, long resultVersion, Instant createdAt) {
    public RecordedOperation {
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(requestFingerprint, "requestFingerprint");
        Objects.requireNonNull(resultEntityId, "resultEntityId");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
