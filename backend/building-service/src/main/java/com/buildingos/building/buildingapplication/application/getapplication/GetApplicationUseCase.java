package com.buildingos.building.buildingapplication.application.getapplication;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

public interface GetApplicationUseCase {
    BuildingApplication execute(Actor actor, GetApplicationQuery query);
}
