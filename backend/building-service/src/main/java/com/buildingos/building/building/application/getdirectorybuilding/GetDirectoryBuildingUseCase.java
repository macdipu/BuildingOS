package com.buildingos.building.building.application.getdirectorybuilding;

import com.buildingos.building.building.domain.model.Building;

public interface GetDirectoryBuildingUseCase {
    Building execute(GetDirectoryBuildingQuery query);
}
