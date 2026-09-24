package com.buildingos.building.ownership.application;

import com.buildingos.building.ownership.domain.model.OwnershipPeriod;
import com.buildingos.building.ownership.domain.model.OwnershipTransfer;
import java.util.List;
import java.util.UUID;

/** Unit allocations after a change (or now, for a replay); {@code transfer} is null for an assignment. */
public record OwnershipResult(UUID unitId, long revision, List<OwnershipPeriod> current, OwnershipTransfer transfer,
        boolean replayed) {
    public OwnershipResult {
        current = List.copyOf(current);
    }
}
