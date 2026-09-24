package com.buildingos.building.buildingapplication.application.getapplicationhistory;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.util.List;

/** Status history for the applicant and platform admins. Internal notes are a separate, admin-only resource. */
public final class GetApplicationHistoryService implements GetApplicationHistoryUseCase {
    private final ApplicationChanges changes;
    private final LifecycleTransitionRepository transitions;

    public GetApplicationHistoryService(ApplicationChanges changes, LifecycleTransitionRepository transitions) {
        this.changes = changes;
        this.transitions = transitions;
    }

    @Override
    public List<LifecycleTransition> execute(Actor actor, GetApplicationHistoryQuery query) {
        var application = changes.load(actor, query.applicationId(), ApplicationAccess.APPLICANT_OR_PLATFORM_ADMIN);
        return transitions.findFor(EntityType.BUILDING_APPLICATION, application.id());
    }
}
