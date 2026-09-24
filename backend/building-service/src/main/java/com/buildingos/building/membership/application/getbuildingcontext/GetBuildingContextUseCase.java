package com.buildingos.building.membership.application.getbuildingcontext;

import com.buildingos.building.shared.application.Actor;

public interface GetBuildingContextUseCase {
    BuildingContext execute(Actor actor, GetBuildingContextQuery query);
}
