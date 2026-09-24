package com.buildingos.building.unit.domain.model;

import com.buildingos.building.shared.domain.model.StaleVersionException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Floor(UUID id, UUID buildingId, FloorDetails details, long version, Instant createdAt,
        Instant updatedAt) {
    public Floor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Floor create(UUID buildingId, FloorDetails details, Instant at) {
        return new Floor(UUID.randomUUID(), buildingId, details, 0, at, at);
    }

    public Floor updated(FloorDetails next, long expectedVersion, Instant at) {
        if (version != expectedVersion) {
            throw new StaleVersionException("Floor");
        }
        return new Floor(id, buildingId, next, version + 1, createdAt, at);
    }
}
