package com.buildingos.building.unit.application.updateunit;

import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.shared.application.Actor;

public interface UpdateUnitUseCase {
    UnitView execute(Actor actor, UpdateUnitCommand command);
}
