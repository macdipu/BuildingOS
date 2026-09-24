package com.buildingos.building.building.application.activatebuilding;

import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.shared.application.Actor;

public interface ActivateBuildingUseCase {
    Building execute(Actor actor, BuildingLifecycleCommand command);
}
