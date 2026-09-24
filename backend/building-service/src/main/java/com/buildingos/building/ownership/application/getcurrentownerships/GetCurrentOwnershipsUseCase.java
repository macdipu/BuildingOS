package com.buildingos.building.ownership.application.getcurrentownerships;

import com.buildingos.building.ownership.domain.model.UnitOwnership;
import com.buildingos.building.shared.application.Actor;

public interface GetCurrentOwnershipsUseCase {
    UnitOwnership execute(Actor actor, GetCurrentOwnershipsQuery query);
}
