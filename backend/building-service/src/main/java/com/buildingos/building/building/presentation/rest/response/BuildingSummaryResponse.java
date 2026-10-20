package com.buildingos.building.building.presentation.rest.response;

import com.buildingos.building.building.domain.model.Building;
import java.time.Instant;
import java.util.UUID;

/** List row; memberships are on the detail response. */
public record BuildingSummaryResponse(UUID id, UUID applicationId, String name, String buildingType, String area,
        String district, String status, Instant createdAt, Instant updatedAt) {
    public static BuildingSummaryResponse of(Building b) {
        return new BuildingSummaryResponse(b.id(), b.applicationId(), b.name(), b.type().name(), b.area(),
                b.district(), b.status().name(), b.createdAt(), b.updatedAt());
    }
}
