package com.buildingos.building.ownership.presentation.rest.response;

import com.buildingos.building.ownership.domain.model.OwnedProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** No rent, occupancy or dues: those belong to later services (UO-12). */
public record PropertyResponse(UUID buildingId, String buildingName, UUID unitId, String unitNumber, String floorLabel,
        String unitType, BigDecimal areaSqft, BigDecimal share, Instant since) {
    public static PropertyResponse of(OwnedProperty p) {
        return new PropertyResponse(p.buildingId(), p.buildingName(), p.unitId(), p.unitNumber(), p.floorLabel(),
                p.unitType().name(), p.areaSqft(), p.share().percent(), p.since());
    }
}
