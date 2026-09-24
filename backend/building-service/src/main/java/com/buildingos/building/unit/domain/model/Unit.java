package com.buildingos.building.unit.domain.model;

import com.buildingos.building.shared.domain.model.StaleVersionException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** An individual building unit; occupancy, rent and dues belong to later services (UO-02). */
public record Unit(UUID id, UUID buildingId, UnitDetails details, long version, Instant createdAt,
        Instant updatedAt) {
    public Unit {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Unit create(UUID buildingId, UnitDetails details, Instant at) {
        return new Unit(UUID.randomUUID(), buildingId, details, 0, at, at);
    }

    public Unit updated(UnitDetails next, long expectedVersion, Instant at) {
        if (version != expectedVersion) {
            throw new StaleVersionException("Unit");
        }
        return new Unit(id, buildingId, next, version + 1, createdAt, at);
    }
}
