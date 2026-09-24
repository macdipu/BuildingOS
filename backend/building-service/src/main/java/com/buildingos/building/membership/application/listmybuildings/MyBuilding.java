package com.buildingos.building.membership.application.listmybuildings;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingRole;
import java.util.Set;

public record MyBuilding(Building building, Set<BuildingRole> roles) {
    public MyBuilding {
        roles = Set.copyOf(roles);
    }
}
