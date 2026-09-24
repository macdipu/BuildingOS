package com.buildingos.building.ownership.domain.model;

import java.util.List;

public record OwnershipHistory(long revision, List<OwnershipPeriod> periods, List<OwnershipTransfer> transfers) {
    public OwnershipHistory {
        periods = List.copyOf(periods);
        transfers = List.copyOf(transfers);
    }
}
