package com.buildingos.building.building.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A global user's role in one building (BRD 110.1); separate from ownership and tenancy. building-service is the
 * sole membership authority (ADR-F4-001). Revocation fields are set only while {@code REVOKED}.
 */
public record BuildingMembership(UUID id, UUID buildingId, UUID userId, BuildingRole role, MembershipStatus status,
        long version, Instant createdAt, Instant updatedAt, Instant revokedAt, UUID revokedBy,
        String revocationReason) {
    public BuildingMembership {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static BuildingMembership admin(UUID buildingId, UUID userId, Instant at) {
        return active(buildingId, userId, BuildingRole.BUILDING_ADMIN, at);
    }

    public static BuildingMembership owner(UUID buildingId, UUID userId, Instant at) {
        return active(buildingId, userId, BuildingRole.OWNER, at);
    }

    private static BuildingMembership active(UUID buildingId, UUID userId, BuildingRole role, Instant at) {
        return new BuildingMembership(UUID.randomUUID(), buildingId, userId, role, MembershipStatus.ACTIVE, 0, at, at,
                null, null, null);
    }

    public boolean isActive() { return status == MembershipStatus.ACTIVE; }

    public boolean isActiveAdmin() { return isActive() && role == BuildingRole.BUILDING_ADMIN; }

    /** Only OWNER memberships are revocable through F4; admin removal needs a separate permission design. */
    public BuildingMembership revoked(UUID by, String reason, long expectedVersion, Instant at) {
        if (version != expectedVersion) {
            throw new MembershipStateException("STALE_VERSION", "Membership changed; reload and retry");
        }
        if (role != BuildingRole.OWNER) {
            throw new MembershipStateException("MEMBERSHIP_NOT_REVOCABLE", "Only owner memberships can be revoked");
        }
        if (!isActive()) {
            throw new MembershipStateException("MEMBERSHIP_NOT_ACTIVE", "Membership is already revoked");
        }
        return new BuildingMembership(id, buildingId, userId, role, MembershipStatus.REVOKED, version + 1, createdAt,
                at, at, by, reason);
    }

    /** A newly claimed invitation re-enables a revoked row; the unique building/user/role key keeps one row. */
    public BuildingMembership reinstated(Instant at) {
        return new BuildingMembership(id, buildingId, userId, role, MembershipStatus.ACTIVE, version + 1, createdAt,
                at, null, null, null);
    }
}
