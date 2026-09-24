package com.buildingos.building.building.application.reactivatebuilding;

import com.buildingos.building.building.application.BuildingChanges;
import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.domain.model.ActivationReadiness;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.unit.domain.repository.UnitRepository;

/** SUSPENDED back to ACTIVE; rechecks the activation prerequisites (DECISIONS "Building status"). */
public final class ReactivateBuildingService implements ReactivateBuildingUseCase {
    private final BuildingChanges changes;
    private final BuildingMembershipRepository memberships;
    private final UnitRepository units;

    public ReactivateBuildingService(BuildingChanges changes, BuildingMembershipRepository memberships,
            UnitRepository units) {
        this.changes = changes;
        this.memberships = memberships;
        this.units = units;
    }

    @Override
    public Building execute(Actor actor, BuildingLifecycleCommand command) {
        return changes.apply(actor, command.buildingId(), command.reason(), (current, now) -> current.reactivated(
                new ActivationReadiness(memberships.countActiveAdmins(current.id()), units.countByBuilding(current.id())),
                now));
    }
}
