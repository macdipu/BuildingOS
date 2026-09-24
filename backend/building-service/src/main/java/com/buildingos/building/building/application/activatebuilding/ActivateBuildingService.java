package com.buildingos.building.building.application.activatebuilding;

import com.buildingos.building.building.application.BuildingChanges;
import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.domain.model.ActivationReadiness;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.unit.domain.repository.UnitRepository;

/** ONBOARDING to ACTIVE once an active building admin and a unit exist (D-30, UO-09); read under the building lock. */
public final class ActivateBuildingService implements ActivateBuildingUseCase {
    private final BuildingChanges changes;
    private final BuildingMembershipRepository memberships;
    private final UnitRepository units;

    public ActivateBuildingService(BuildingChanges changes, BuildingMembershipRepository memberships,
            UnitRepository units) {
        this.changes = changes;
        this.memberships = memberships;
        this.units = units;
    }

    @Override
    public Building execute(Actor actor, BuildingLifecycleCommand command) {
        return changes.apply(actor, command.buildingId(), command.reason(), (current, now) -> current.activated(
                new ActivationReadiness(memberships.countActiveAdmins(current.id()), units.countByBuilding(current.id())),
                now));
    }
}
