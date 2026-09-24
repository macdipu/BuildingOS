package com.buildingos.building.buildingapplication.application.submitapplication;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

public interface SubmitApplicationUseCase {
    BuildingApplication execute(Actor actor, SubmitApplicationCommand command);
}
