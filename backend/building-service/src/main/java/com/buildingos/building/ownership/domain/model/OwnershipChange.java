package com.buildingos.building.ownership.domain.model;

import java.util.List;

/** Periods closed and opened by one ordered revision; {@code transfer} is null for an assignment. */
public record OwnershipChange(long previousRevision, long revision, List<OwnershipPeriod> closed,
        List<OwnershipPeriod> opened, OwnershipTransfer transfer) {
    public OwnershipChange {
        closed = List.copyOf(closed);
        opened = List.copyOf(opened);
    }
}
