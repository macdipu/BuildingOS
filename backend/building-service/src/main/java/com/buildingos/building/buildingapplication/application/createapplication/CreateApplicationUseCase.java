package com.buildingos.building.buildingapplication.application.createapplication;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

public interface CreateApplicationUseCase {
    BuildingApplication execute(Actor actor, CreateApplicationCommand command);
}
