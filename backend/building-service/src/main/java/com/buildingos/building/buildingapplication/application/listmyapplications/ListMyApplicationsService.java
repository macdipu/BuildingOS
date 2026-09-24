package com.buildingos.building.buildingapplication.application.listmyapplications;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public final class ListMyApplicationsService implements ListMyApplicationsUseCase {
    private final BuildingApplicationRepository applications;

    public ListMyApplicationsService(BuildingApplicationRepository applications) { this.applications = applications; }

    @Override
    public List<BuildingApplication> execute(Actor actor) {
        return applications.findByApplicant(actor.userId());
    }
}
