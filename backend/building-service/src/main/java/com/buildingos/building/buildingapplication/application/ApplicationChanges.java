package com.buildingos.building.buildingapplication.application;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Loads, locks, changes and stores an application; a status change also appends its audited transition (AP-04). */
public final class ApplicationChanges {
    private final BuildingApplicationRepository applications;
    private final LifecycleTransitionRepository transitions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ApplicationChanges(BuildingApplicationRepository applications, LifecycleTransitionRepository transitions,
            UnitOfWork unitOfWork, Clock clock) {
        this.applications = applications;
        this.transitions = transitions;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    public BuildingApplication apply(Actor actor, UUID applicationId, ApplicationAccess access,
            BiFunction<BuildingApplication, Instant, BuildingApplication> change,
            Function<BuildingApplication, String> reason) {
        if (access == ApplicationAccess.PLATFORM_ADMIN) {
            actor.requirePlatformAdmin();
        }
        return unitOfWork.inTransaction(() -> {
            var current = applications.findByIdForUpdate(applicationId)
                    .filter(found -> access.allows(actor, found))
                    .orElseThrow(() -> ApplicationErrors.notFound(applicationId));
            var now = clock.instant();
            var next = change.apply(current, now);
            applications.update(next);
            if (next.status() != current.status()) {
                transitions.append(LifecycleTransition.of(EntityType.BUILDING_APPLICATION, applicationId,
                        current.status(), next.status(), actor.userId(), reason.apply(next), now));
            }
            return next;
        });
    }

    public BuildingApplication load(Actor actor, UUID applicationId, ApplicationAccess access) {
        if (access == ApplicationAccess.PLATFORM_ADMIN) {
            actor.requirePlatformAdmin();
        }
        return applications.findById(applicationId)
                .filter(found -> access.allows(actor, found))
                .orElseThrow(() -> ApplicationErrors.notFound(applicationId));
    }
}
