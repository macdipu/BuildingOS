package com.buildingos.building.buildingapplication.application.createapplication;

import com.buildingos.building.buildingapplication.domain.model.ApplicationNumber;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

/** Any authenticated user may start a draft; they become its applicant, not its building admin (BRD 149.3). */
public final class CreateApplicationService implements CreateApplicationUseCase {
    private final BuildingApplicationRepository applications;
    private final LifecycleTransitionRepository transitions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CreateApplicationService(BuildingApplicationRepository applications,
            LifecycleTransitionRepository transitions, UnitOfWork unitOfWork, Clock clock) {
        this.applications = applications;
        this.transitions = transitions;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public BuildingApplication execute(Actor actor, CreateApplicationCommand command) {
        return unitOfWork.inTransaction(() -> {
            var now = clock.instant();
            var number = ApplicationNumber.of(now.atZone(ZoneOffset.UTC).getYear(), applications.nextNumberSequence());
            var draft = BuildingApplication.draft(UUID.randomUUID(), number, actor.userId(), command.details(), now);
            applications.insert(draft);
            transitions.append(LifecycleTransition.of(EntityType.BUILDING_APPLICATION, draft.id(), null, draft.status(),
                    actor.userId(), null, now));
            return draft;
        });
    }
}
