package com.buildingos.building.unit.application.batch;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Floor/count/pattern generation, or duplication of {@code templateFloorId}'s layout onto {@code floorIds}
 * (then count and unit attributes come from the template). {@code start} offsets {n}/{nn} (default 1).
 */
public record GenerateSpec(List<UUID> floorIds, Integer unitsPerFloor, String numberPattern, Integer start,
        String type, BigDecimal areaSqft, Integer bedrooms, BigDecimal defaultMaintenanceRate,
        UUID templateFloorId) {}
