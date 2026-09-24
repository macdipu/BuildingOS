package com.buildingos.building.unit.application.listunits;

import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

public interface ListUnitsUseCase {
    Page<UnitView> execute(Actor actor, ListUnitsQuery query);
}
