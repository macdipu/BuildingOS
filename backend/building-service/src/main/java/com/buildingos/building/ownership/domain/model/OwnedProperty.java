package com.buildingos.building.ownership.domain.model;

import com.buildingos.building.unit.domain.model.UnitType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One of the caller's current allocations, with the unit/building fields My Properties shows (UO-08). */
public record OwnedProperty(UUID buildingId, String buildingName, UUID unitId, String unitNumber, String floorLabel,
        UnitType unitType, BigDecimal areaSqft, Share share, Instant since) {
    public OwnedProperty {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(unitId, "unitId");
        Objects.requireNonNull(share, "share");
    }
}
