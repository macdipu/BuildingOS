package com.buildingos.building.ownership.application.listmyproperties;

import com.buildingos.building.ownership.domain.model.OwnedProperty;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

public interface ListMyPropertiesUseCase {
    Page<OwnedProperty> execute(Actor actor, ListMyPropertiesQuery query);
}
