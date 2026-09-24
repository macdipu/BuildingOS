package com.buildingos.building.building.application.getbuilding;

import com.buildingos.building.shared.application.Actor;

public interface GetBuildingUseCase {
    BuildingView execute(Actor actor, GetBuildingQuery query);
}
