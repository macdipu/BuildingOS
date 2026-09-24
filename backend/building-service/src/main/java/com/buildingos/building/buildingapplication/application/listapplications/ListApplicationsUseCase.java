package com.buildingos.building.buildingapplication.application.listapplications;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

public interface ListApplicationsUseCase {
    Page<BuildingApplication> execute(Actor actor, ListApplicationsQuery query);
}
