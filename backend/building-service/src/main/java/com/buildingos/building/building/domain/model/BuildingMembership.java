package com.buildingos.building.building.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A global user's role in one building (BRD 110.1); separate from ownership and tenancy. */
public record BuildingMembership(UUID id, UUID buildingId, UUID userId, BuildingRole role, MembershipStatus status,
        Instant createdAt) {
    public BuildingMembership {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static BuildingMembership admin(UUID buildingId, UUID userId, Instant at) {
        return new BuildingMembership(UUID.randomUUID(), buildingId, userId, BuildingRole.BUILDING_ADMIN,
                MembershipStatus.ACTIVE, at);
    }
}
