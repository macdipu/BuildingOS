package com.buildingos.building.buildingapplication.application.requestinformation;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

/** UNDER_REVIEW to MORE_INFORMATION_REQUIRED; the message is shown to the applicant. */
public final class RequestInformationService implements RequestInformationUseCase {
    private final ApplicationChanges changes;

    public RequestInformationService(ApplicationChanges changes) { this.changes = changes; }

    @Override
    public BuildingApplication execute(Actor actor, RequestInformationCommand command) {
        return changes.apply(actor, command.applicationId(), ApplicationAccess.PLATFORM_ADMIN,
                (current, now) -> current.informationRequested(actor.userId(), command.message(), now), BuildingApplication::infoRequestMessage);
    }
}
