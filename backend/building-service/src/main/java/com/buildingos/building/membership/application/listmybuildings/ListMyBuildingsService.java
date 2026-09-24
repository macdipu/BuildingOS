package com.buildingos.building.membership.application.listmybuildings;

import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import java.util.stream.Collectors;

/** My Buildings: only current memberships; supplied IDs never grant access (UO-01). */
public final class ListMyBuildingsService implements ListMyBuildingsUseCase {
    private final BuildingRepository buildings;
    private final BuildingMembershipRepository memberships;
    private final OwnershipRepository ownership;
    private final UnitOfWork unitOfWork;

    public ListMyBuildingsService(BuildingRepository buildings, BuildingMembershipRepository memberships,
            OwnershipRepository ownership, UnitOfWork unitOfWork) {
        this.buildings = buildings;
        this.memberships = memberships;
        this.ownership = ownership;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Page<MyBuilding> execute(Actor actor, ListMyBuildingsQuery query) {
        Page.validate(query.page(), query.size());
        return unitOfWork.inTransaction(() -> {
            var items = memberships.findActiveBuildingIds(actor.userId(), query.page(), query.size()).stream()
                    .flatMap(id -> buildings.findById(id).stream())
                    .map(b -> new MyBuilding(b, memberships.findActive(b.id(), actor.userId()).stream()
                            .map(BuildingMembership::role).collect(Collectors.toSet()),
                            ownership.countOwnedUnits(b.id(), actor.userId())))
                    .toList();
            return new Page<>(items, query.page(), query.size(), memberships.countActiveBuildings(actor.userId()));
        });
    }
}
