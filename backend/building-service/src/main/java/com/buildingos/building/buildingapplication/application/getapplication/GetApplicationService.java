package com.buildingos.building.buildingapplication.application.getapplication;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

public final class GetApplicationService implements GetApplicationUseCase {
    private final ApplicationChanges changes;

    public GetApplicationService(ApplicationChanges changes) { this.changes = changes; }

    @Override
    public BuildingApplication execute(Actor actor, GetApplicationQuery query) {
        return changes.load(actor, query.applicationId(), ApplicationAccess.APPLICANT_OR_PLATFORM_ADMIN);
    }
}
