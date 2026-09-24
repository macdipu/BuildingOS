package com.buildingos.building.buildingapplication.application.updateapplication;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

public interface UpdateApplicationUseCase {
    BuildingApplication execute(Actor actor, UpdateApplicationCommand command);
}
