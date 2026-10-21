package com.buildingos.building.building.presentation.rest.response;

import com.buildingos.building.building.domain.model.Building;
import java.util.UUID;

/** Directory view for service callers (D-38): id and lifecycle status only. */
public record DirectoryBuildingResponse(UUID id, String status) {
    public static DirectoryBuildingResponse of(Building building) {
        return new DirectoryBuildingResponse(building.id(), building.status().name());
    }
}
