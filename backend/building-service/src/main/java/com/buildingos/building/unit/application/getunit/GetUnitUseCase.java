package com.buildingos.building.unit.application.getunit;

import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.shared.application.Actor;

public interface GetUnitUseCase {
    UnitView execute(Actor actor, GetUnitQuery query);
}
