package com.buildingos.building.ownership.application.assignownership;

import com.buildingos.building.ownership.application.OwnershipResult;
import com.buildingos.building.shared.application.Actor;

public interface AssignOwnershipUseCase {
    OwnershipResult execute(Actor actor, AssignOwnershipCommand command);
}
