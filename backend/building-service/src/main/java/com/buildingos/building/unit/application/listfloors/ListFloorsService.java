package com.buildingos.building.unit.application.listfloors;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.repository.FloorRepository;

public final class ListFloorsService implements ListFloorsUseCase {
    private final BuildingAccess access;
    private final FloorRepository floors;
    private final UnitOfWork unitOfWork;

    public ListFloorsService(BuildingAccess access, FloorRepository floors, UnitOfWork unitOfWork) {
        this.access = access;
        this.floors = floors;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Page<Floor> execute(Actor actor, ListFloorsQuery query) {
        Page.validate(query.page(), query.size());
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToRead(actor, query.buildingId());
            return new Page<>(floors.findByBuilding(query.buildingId(), query.page(), query.size()), query.page(),
                    query.size(), floors.countByBuilding(query.buildingId()));
        });
    }
}
