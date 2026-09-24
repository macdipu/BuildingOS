package com.buildingos.building.membership.application.listmybuildings;

import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

public interface ListMyBuildingsUseCase {
    Page<MyBuilding> execute(Actor actor, ListMyBuildingsQuery query);
}
