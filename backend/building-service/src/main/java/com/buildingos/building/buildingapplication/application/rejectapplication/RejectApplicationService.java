package com.buildingos.building.buildingapplication.application.rejectapplication;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

/** UNDER_REVIEW to REJECTED with a required reason (BRD 149.4). */
public final class RejectApplicationService implements RejectApplicationUseCase {
    private final ApplicationChanges changes;

    public RejectApplicationService(ApplicationChanges changes) { this.changes = changes; }

    @Override
    public BuildingApplication execute(Actor actor, RejectApplicationCommand command) {
        return changes.apply(actor, command.applicationId(), ApplicationAccess.PLATFORM_ADMIN,
                (current, now) -> current.rejected(actor.userId(), command.reason(), now), BuildingApplication::rejectionReason);
    }
}
