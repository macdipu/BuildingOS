package com.buildingos.building.building.application.getdirectorybuilding;

import com.buildingos.building.building.application.BuildingErrors;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.shared.application.NotPermittedException;

/** Existence + lifecycle status of one building for service callers holding {@code directory.read} (BOS-010 D-38). */
public final class GetDirectoryBuildingService implements GetDirectoryBuildingUseCase {
    public static final String DIRECTORY_READ = "directory.read";
    private final BuildingRepository buildings;

    public GetDirectoryBuildingService(BuildingRepository buildings) {
        this.buildings = buildings;
    }

    @Override
    public Building execute(GetDirectoryBuildingQuery query) {
        if (!query.callerServiceScopes().contains(DIRECTORY_READ)) {
            throw new NotPermittedException();
        }
        return buildings.findById(query.buildingId()).orElseThrow(() -> BuildingErrors.notFound(query.buildingId()));
    }
}
