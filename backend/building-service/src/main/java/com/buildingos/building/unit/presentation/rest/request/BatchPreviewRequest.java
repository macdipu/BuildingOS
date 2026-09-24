package com.buildingos.building.unit.presentation.rest.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Exactly one of {@code rows} or {@code generate}. */
public record BatchPreviewRequest(List<BatchRowRequest> rows, Generate generate) {
    public record Generate(List<UUID> floorIds, Integer unitsPerFloor, String numberPattern, Integer start,
            String type, BigDecimal areaSqft, Integer bedrooms, BigDecimal defaultMaintenanceRate,
            UUID templateFloorId) {}
}
