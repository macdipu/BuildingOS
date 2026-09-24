package com.buildingos.building.unit.application.listfloors;

import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.unit.domain.model.Floor;

public interface ListFloorsUseCase {
    Page<Floor> execute(Actor actor, ListFloorsQuery query);
}
