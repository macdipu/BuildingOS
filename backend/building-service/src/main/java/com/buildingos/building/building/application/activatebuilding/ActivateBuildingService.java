package com.buildingos.building.building.application.activatebuilding;

import com.buildingos.building.building.application.BuildingChanges;
import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.shared.application.Actor;

/** ONBOARDING to ACTIVE once an active building admin exists (interim rule D-30). */
public final class ActivateBuildingService implements ActivateBuildingUseCase {
    private final BuildingChanges changes;
    private final BuildingMembershipRepository memberships;

    public ActivateBuildingService(BuildingChanges changes, BuildingMembershipRepository memberships) {
        this.changes = changes;
        this.memberships = memberships;
    }

    @Override
    public Building execute(Actor actor, BuildingLifecycleCommand command) {
        return changes.apply(actor, command.buildingId(), command.reason(), (current, now) -> current.activated(memberships.countActiveAdmins(current.id()), now));
    }
}
