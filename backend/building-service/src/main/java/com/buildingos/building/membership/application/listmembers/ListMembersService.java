package com.buildingos.building.membership.application.listmembers;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.application.port.out.UnitOfWork;

public final class ListMembersService implements ListMembersUseCase {
    private final BuildingAccess access;
    private final BuildingMembershipRepository memberships;
    private final UnitOfWork unitOfWork;

    public ListMembersService(BuildingAccess access, BuildingMembershipRepository memberships, UnitOfWork unitOfWork) {
        this.access = access;
        this.memberships = memberships;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Page<BuildingMembership> execute(Actor actor, ListMembersQuery query) {
        Page.validate(query.page(), query.size());
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToRead(actor, query.buildingId());
            return new Page<>(memberships.findByBuilding(query.buildingId(), query.page(), query.size()),
                    query.page(), query.size(), memberships.countByBuilding(query.buildingId()));
        });
    }
}
