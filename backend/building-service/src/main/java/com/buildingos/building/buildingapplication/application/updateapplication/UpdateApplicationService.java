package com.buildingos.building.buildingapplication.application.updateapplication;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

/** Applicant edits while DRAFT or MORE_INFORMATION_REQUIRED; not a state change, so no transition row. */
public final class UpdateApplicationService implements UpdateApplicationUseCase {
    private final ApplicationChanges changes;

    public UpdateApplicationService(ApplicationChanges changes) { this.changes = changes; }

    @Override
    public BuildingApplication execute(Actor actor, UpdateApplicationCommand command) {
        return changes.apply(actor, command.applicationId(), ApplicationAccess.APPLICANT,
                (current, now) -> current.edited(command.details(), now), next -> null);
    }
}
