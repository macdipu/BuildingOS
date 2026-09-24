package com.buildingos.building.ownership.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One owner's allocation on a unit between two ordered unit revisions. Closing fills the end marker once; owner,
 * share and start are never rewritten, so every historical state is preserved (UO-07).
 */
public record OwnershipPeriod(UUID id, UUID buildingId, UUID unitId, UUID ownerUserId, Share share, Instant startAt,
        long startRevision, Instant endAt, Long endRevision, String notes, UUID createdBy) {
    public OwnershipPeriod {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(share, "share");
        Objects.requireNonNull(startAt, "startAt");
        Objects.requireNonNull(createdBy, "createdBy");
    }

    static OwnershipPeriod open(UUID buildingId, UUID unitId, UUID owner, Share share, Instant at, long revision,
            String notes, UUID actor) {
        return new OwnershipPeriod(UUID.randomUUID(), buildingId, unitId, owner, share, at, revision, null, null, notes,
                actor);
    }

    public boolean isOpen() { return endRevision == null; }

    OwnershipPeriod closed(Instant at, long revision) {
        return new OwnershipPeriod(id, buildingId, unitId, ownerUserId, share, startAt, startRevision, at, revision,
                notes, createdBy);
    }
}
