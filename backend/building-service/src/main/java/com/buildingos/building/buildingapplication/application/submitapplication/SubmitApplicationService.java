package com.buildingos.building.buildingapplication.application.submitapplication;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

/** DRAFT or MORE_INFORMATION_REQUIRED to SUBMITTED once every required field is filled. */
public final class SubmitApplicationService implements SubmitApplicationUseCase {
    private final ApplicationChanges changes;

    public SubmitApplicationService(ApplicationChanges changes) { this.changes = changes; }

    @Override
    public BuildingApplication execute(Actor actor, SubmitApplicationCommand command) {
        return changes.apply(actor, command.applicationId(), ApplicationAccess.APPLICANT,
                (current, now) -> current.submitted(now), next -> null);
    }
}
