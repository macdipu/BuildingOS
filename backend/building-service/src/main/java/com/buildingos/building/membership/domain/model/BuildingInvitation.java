package com.buildingos.building.membership.domain.model;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * OWNER invitation to a canonical phone (TECH-SPEC-F4). Claim binds it to the verified subject; it never grants
 * ownership. A PENDING invitation past {@code expiresAt} is treated as EXPIRED.
 */
public record BuildingInvitation(UUID id, UUID buildingId, ContactPhone phone, BuildingRole role,
        InvitationStatus status, UUID createdBy, String reason, Instant createdAt, Instant expiresAt, long version,
        UUID claimedUserId, Instant claimedAt, UUID revokedBy, Instant revokedAt, String revocationReason) {
    public BuildingInvitation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(phone, "phone");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public static BuildingInvitation owner(UUID buildingId, ContactPhone phone, UUID createdBy, String reason,
            Instant at, Duration ttl) {
        return new BuildingInvitation(UUID.randomUUID(), buildingId, phone, BuildingRole.OWNER,
                InvitationStatus.PENDING, createdBy, reason, at, at.plus(ttl), 0, null, null, null, null, null);
    }

    public InvitationStatus effectiveStatus(Instant now) {
        return status == InvitationStatus.PENDING && !now.isBefore(expiresAt) ? InvitationStatus.EXPIRED : status;
    }

    public boolean isLive(Instant now) {
        return effectiveStatus(now) == InvitationStatus.PENDING;
    }

    public BuildingInvitation expired() {
        return with(InvitationStatus.EXPIRED, null, null, null, null, null);
    }

    public BuildingInvitation claimed(UUID userId, Instant at) {
        requireLive(at);
        return with(InvitationStatus.CLAIMED, userId, at, null, null, null);
    }

    public BuildingInvitation revoked(UUID by, String revocation, Instant at) {
        requireLive(at);
        return with(InvitationStatus.REVOKED, null, null, by, at, revocation);
    }

    private void requireLive(Instant now) {
        switch (effectiveStatus(now)) {
            case PENDING -> { }
            case EXPIRED -> throw new InvitationStateException("INVITATION_EXPIRED", "Invitation has expired");
            case REVOKED -> throw new InvitationStateException("INVITATION_REVOKED", "Invitation was revoked");
            case CLAIMED -> throw new InvitationStateException("INVITATION_ALREADY_CLAIMED",
                    "Invitation was already claimed");
        }
    }

    private BuildingInvitation with(InvitationStatus next, UUID claimedUser, Instant claimedTime, UUID revoker,
            Instant revokedTime, String revocation) {
        return new BuildingInvitation(id, buildingId, phone, role, next, createdBy, reason, createdAt, expiresAt,
                version + 1, claimedUser, claimedTime, revoker, revokedTime, revocation);
    }
}
