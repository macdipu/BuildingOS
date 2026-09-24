package com.buildingos.building.building.application.reactivatebuilding;

import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.shared.application.Actor;

public interface ReactivateBuildingUseCase {
    Building execute(Actor actor, BuildingLifecycleCommand command);
}
