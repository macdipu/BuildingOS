package com.buildingos.building.building.application.listplatformbuildings;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

public interface ListPlatformBuildingsUseCase {
    Page<Building> execute(Actor actor, ListPlatformBuildingsQuery query);
}
