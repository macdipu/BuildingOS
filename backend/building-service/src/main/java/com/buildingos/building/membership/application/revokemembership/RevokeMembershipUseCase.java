package com.buildingos.building.membership.application.revokemembership;

import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.shared.application.Actor;

public interface RevokeMembershipUseCase {
    BuildingMembership execute(Actor actor, RevokeMembershipCommand command);
}
