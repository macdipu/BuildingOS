package com.buildingos.building.building.application;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;

/** Platform-admin lifecycle action on a locked building with a required audit reason (BRD 149.5, AP-09). */
public final class BuildingChanges {
    private final BuildingRepository buildings;
    private final LifecycleTransitionRepository transitions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public BuildingChanges(BuildingRepository buildings, LifecycleTransitionRepository transitions,
            UnitOfWork unitOfWork, Clock clock) {
        this.buildings = buildings;
        this.transitions = transitions;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    public Building apply(Actor actor, UUID buildingId, String reason,
            BiFunction<Building, Instant, Building> change) {
        actor.requirePlatformAdmin();
        String auditReason = BuildingApplication.reason(reason, "reason");
        return unitOfWork.inTransaction(() -> {
            var current = buildings.findByIdForUpdate(buildingId).orElseThrow(() -> BuildingErrors.notFound(buildingId));
            var now = clock.instant();
            var next = change.apply(current, now);
            buildings.update(next);
            transitions.append(LifecycleTransition.of(EntityType.BUILDING, buildingId, current.status(), next.status(),
                    actor.userId(), auditReason, now));
            return next;
        });
    }
}
