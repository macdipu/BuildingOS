package com.buildingos.building.unit.application.createunit;

import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.shared.application.Actor;

public interface CreateUnitUseCase {
    UnitView execute(Actor actor, CreateUnitCommand command);
}
