package com.buildingos.building.unit.domain.model;

import java.util.UUID;

/**
 * Unit list criteria (BRD §48). Null fields = any. {@code numberContains} is matched against the normalized
 * (upper-cased) unit number.
 */
public record UnitSearch(UUID floorId, UnitType type, UUID ownerUserId, String numberContains, UnitSort sort) {
    public UnitSearch {
        sort = sort == null ? UnitSort.DEFAULT : sort;
    }

    public static UnitSearch of(UUID floorId, UnitType type, UUID ownerUserId) {
        return new UnitSearch(floorId, type, ownerUserId, null, UnitSort.DEFAULT);
    }

    public UnitSearch withOwner(UUID owner) {
        return new UnitSearch(floorId, type, owner, numberContains, sort);
    }
}
