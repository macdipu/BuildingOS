package com.buildingos.building.buildingapplication.application.listmyapplications;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public interface ListMyApplicationsUseCase {
    List<BuildingApplication> execute(Actor actor);
}
