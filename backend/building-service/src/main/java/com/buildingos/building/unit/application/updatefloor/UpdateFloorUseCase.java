package com.buildingos.building.unit.application.updatefloor;

import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.unit.domain.model.Floor;

public interface UpdateFloorUseCase {
    Floor execute(Actor actor, UpdateFloorCommand command);
}
