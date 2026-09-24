package com.buildingos.building.building.application.reactivatebuilding;

import com.buildingos.building.building.application.BuildingChanges;
import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.shared.application.Actor;

/** SUSPENDED back to ACTIVE. */
public final class ReactivateBuildingService implements ReactivateBuildingUseCase {
    private final BuildingChanges changes;

    public ReactivateBuildingService(BuildingChanges changes) {
        this.changes = changes;
    }

    @Override
    public Building execute(Actor actor, BuildingLifecycleCommand command) {
        return changes.apply(actor, command.buildingId(), command.reason(), (current, now) -> current.reactivated(now));
    }
}
