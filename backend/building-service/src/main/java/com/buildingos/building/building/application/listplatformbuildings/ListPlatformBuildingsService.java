package com.buildingos.building.building.application.listplatformbuildings;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

/** Back-office Buildings lists by lifecycle status (D-34), name ascending. */
public final class ListPlatformBuildingsService implements ListPlatformBuildingsUseCase {
    private final BuildingRepository buildings;

    public ListPlatformBuildingsService(BuildingRepository buildings) { this.buildings = buildings; }

    @Override
    public Page<Building> execute(Actor actor, ListPlatformBuildingsQuery query) {
        actor.requirePlatformAdmin();
        Page.validate(query.page(), query.size());
        String q = query.q() == null || query.q().isBlank() ? null : query.q().trim();
        var items = buildings.findPage(query.status(), q, query.page() * query.size(), query.size());
        return new Page<>(items, query.page(), query.size(), buildings.count(query.status(), q));
    }
}
