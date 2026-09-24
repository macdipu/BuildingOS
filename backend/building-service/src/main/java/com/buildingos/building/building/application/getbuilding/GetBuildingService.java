package com.buildingos.building.building.application.getbuilding;

import com.buildingos.building.building.application.BuildingErrors;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.shared.application.Actor;

public final class GetBuildingService implements GetBuildingUseCase {
    private final BuildingRepository buildings;
    private final BuildingMembershipRepository memberships;

    public GetBuildingService(BuildingRepository buildings, BuildingMembershipRepository memberships) {
        this.buildings = buildings;
        this.memberships = memberships;
    }

    @Override
    public BuildingView execute(Actor actor, GetBuildingQuery query) {
        actor.requirePlatformAdmin();
        var building = buildings.findById(query.buildingId())
                .orElseThrow(() -> BuildingErrors.notFound(query.buildingId()));
        return new BuildingView(building, memberships.findByBuilding(building.id()));
    }
}
