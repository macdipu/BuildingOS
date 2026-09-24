package com.buildingos.building.unit.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Units created together by one confirmed batch commit (UO-04). */
public record UnitBatch(UUID id, UUID buildingId, UUID createdBy, int rowCount, Instant createdAt) {
    public UnitBatch {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static UnitBatch create(UUID buildingId, UUID createdBy, int rowCount, Instant at) {
        return new UnitBatch(UUID.randomUUID(), buildingId, createdBy, rowCount, at);
    }
}
