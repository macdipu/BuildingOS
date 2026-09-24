package com.buildingos.building.buildingapplication.application.requestinformation;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

public interface RequestInformationUseCase {
    BuildingApplication execute(Actor actor, RequestInformationCommand command);
}
