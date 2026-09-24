package com.buildingos.building.membership.application.listmembers;

import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

public interface ListMembersUseCase {
    Page<BuildingMembership> execute(Actor actor, ListMembersQuery query);
}
