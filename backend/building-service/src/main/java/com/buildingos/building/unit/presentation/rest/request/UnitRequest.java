package com.buildingos.building.unit.presentation.rest.request;

import java.math.BigDecimal;
import java.util.UUID;

/** BRD §49 fields; {@code expectedVersion} on update only; {@code reason} is required for platform admins. */
public record UnitRequest(String number, UUID floorId, String type, BigDecimal areaSqft, Integer bedrooms,
        BigDecimal defaultMaintenanceRate, String notes, Long expectedVersion, String reason) {}
