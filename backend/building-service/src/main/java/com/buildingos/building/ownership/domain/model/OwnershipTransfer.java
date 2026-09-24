package com.buildingos.building.ownership.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Immutable record of a share moved from one owner to another at an ordered unit revision (UO-07). */
public record OwnershipTransfer(UUID id, UUID buildingId, UUID unitId, UUID sourceOwnerUserId, UUID recipientUserId,
        Share share, LocalDate effectiveDate, Instant effectiveAt, long revision, UUID actorUserId, String reason,
        String reference) {
    public OwnershipTransfer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(sourceOwnerUserId, "sourceOwnerUserId");
        Objects.requireNonNull(recipientUserId, "recipientUserId");
        Objects.requireNonNull(share, "share");
        Objects.requireNonNull(effectiveDate, "effectiveDate");
        Objects.requireNonNull(effectiveAt, "effectiveAt");
        Objects.requireNonNull(actorUserId, "actorUserId");
        Objects.requireNonNull(reason, "reason");
    }
}
