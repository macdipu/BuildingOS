package com.buildingos.building.ownership.application.transferownership;

import com.buildingos.building.ownership.application.OwnershipResult;
import com.buildingos.building.shared.application.Actor;

public interface TransferOwnershipUseCase {
    OwnershipResult execute(Actor actor, TransferOwnershipCommand command);
}
