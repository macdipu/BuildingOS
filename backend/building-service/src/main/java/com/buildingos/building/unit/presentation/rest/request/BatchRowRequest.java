package com.buildingos.building.unit.presentation.rest.request;

import java.math.BigDecimal;
import java.util.UUID;

/** A reviewed row; the floor is given by {@code floorId} or {@code floorLabel}. */
public record BatchRowRequest(String number, UUID floorId, String floorLabel, String type, BigDecimal areaSqft,
        Integer bedrooms, BigDecimal defaultMaintenanceRate, String notes) {}
