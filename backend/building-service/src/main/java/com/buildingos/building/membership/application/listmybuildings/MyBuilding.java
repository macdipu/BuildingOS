package com.buildingos.building.membership.application.listmybuildings;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingRole;
import java.util.Set;

/** {@code ownedUnitCount} counts the caller's current allocations in this building. */
public record MyBuilding(Building building, Set<BuildingRole> roles, long ownedUnitCount) {
    public MyBuilding {
        roles = Set.copyOf(roles);
    }
}
