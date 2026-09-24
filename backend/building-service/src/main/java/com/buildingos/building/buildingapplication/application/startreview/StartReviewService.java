package com.buildingos.building.buildingapplication.application.startreview;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

/** SUBMITTED to UNDER_REVIEW by a platform admin. */
public final class StartReviewService implements StartReviewUseCase {
    private final ApplicationChanges changes;

    public StartReviewService(ApplicationChanges changes) { this.changes = changes; }

    @Override
    public BuildingApplication execute(Actor actor, StartReviewCommand command) {
        return changes.apply(actor, command.applicationId(), ApplicationAccess.PLATFORM_ADMIN,
                (current, now) -> current.reviewStarted(actor.userId(), now), next -> null);
    }
}
