package com.buildingos.building.building.application.suspendbuilding;

import com.buildingos.building.building.application.BuildingChanges;
import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.shared.application.Actor;

/** ACTIVE to SUSPENDED; deletes nothing (BRD 149.4). */
public final class SuspendBuildingService implements SuspendBuildingUseCase {
    private final BuildingChanges changes;

    public SuspendBuildingService(BuildingChanges changes) {
        this.changes = changes;
    }

    @Override
    public Building execute(Actor actor, BuildingLifecycleCommand command) {
        return changes.apply(actor, command.buildingId(), command.reason(), (current, now) -> current.suspended(now));
    }
}
