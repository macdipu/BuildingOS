package com.buildingos.building.unit.application.createfloor;

import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.unit.domain.model.Floor;

public interface CreateFloorUseCase {
    Floor execute(Actor actor, CreateFloorCommand command);
}
