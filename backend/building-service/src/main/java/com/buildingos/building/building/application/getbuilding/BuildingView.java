package com.buildingos.building.building.application.getbuilding;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingMembership;
import java.util.List;

public record BuildingView(Building building, List<BuildingMembership> memberships) {
    public BuildingView {
        memberships = List.copyOf(memberships);
    }
}
