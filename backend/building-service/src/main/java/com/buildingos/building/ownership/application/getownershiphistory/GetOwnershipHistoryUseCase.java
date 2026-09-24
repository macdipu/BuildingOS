package com.buildingos.building.ownership.application.getownershiphistory;

import com.buildingos.building.ownership.domain.model.OwnershipHistory;
import com.buildingos.building.shared.application.Actor;

public interface GetOwnershipHistoryUseCase {
    OwnershipHistory execute(Actor actor, GetOwnershipHistoryQuery query);
}
