package com.buildingos.building.building.application.getdirectorybuilding;

import java.util.Set;
import java.util.UUID;

public record GetDirectoryBuildingQuery(Set<String> callerServiceScopes, UUID buildingId) {
    public GetDirectoryBuildingQuery {
        callerServiceScopes = Set.copyOf(callerServiceScopes);
    }
}
