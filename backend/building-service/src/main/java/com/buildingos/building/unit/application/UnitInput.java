package com.buildingos.building.unit.application;

import com.buildingos.building.unit.domain.model.UnitDetails;
import com.buildingos.building.unit.domain.model.UnitType;
import java.math.BigDecimal;
import java.util.UUID;

/** Raw BRD §49 unit fields; {@link #details()} applies the shared validator. */
public record UnitInput(String number, UUID floorId, UnitType type, BigDecimal areaSqft, Integer bedrooms,
        BigDecimal defaultMaintenanceRate, String notes) {
    public UnitDetails details() {
        return new UnitDetails(number, floorId, type, areaSqft, bedrooms, defaultMaintenanceRate, notes);
    }
}
