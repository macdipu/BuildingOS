package com.buildingos.building.unit.presentation.rest.response;

import com.buildingos.building.unit.application.UnitView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** No occupancy/rent/due fields until their owning services exist (UO-02). */
public record UnitResponse(UUID id, UUID buildingId, String number, UUID floorId, String floorLabel, String type,
        BigDecimal areaSqft, Integer bedrooms, BigDecimal defaultMaintenanceRate, String notes, long version,
        Instant createdAt, Instant updatedAt) {
    public static UnitResponse of(UnitView view) {
        var u = view.unit();
        var d = u.details();
        return new UnitResponse(u.id(), u.buildingId(), d.number(), d.floorId(), view.floor().details().label(),
                d.type().name(), d.areaSqft(), d.bedrooms(), d.defaultMaintenanceRate(), d.notes(), u.version(),
                u.createdAt(), u.updatedAt());
    }
}
