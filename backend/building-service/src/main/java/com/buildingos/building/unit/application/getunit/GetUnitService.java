package com.buildingos.building.unit.application.getunit;

import com.buildingos.building.unit.application.UnitErrors;
import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;

public final class GetUnitService implements GetUnitUseCase {
    private final BuildingAccess access;
    private final FloorRepository floors;
    private final UnitRepository units;
    private final UnitOfWork unitOfWork;

    public GetUnitService(BuildingAccess access, FloorRepository floors, UnitRepository units, UnitOfWork unitOfWork) {
        this.access = access;
        this.floors = floors;
        this.units = units;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public UnitView execute(Actor actor, GetUnitQuery query) {
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToRead(actor, query.buildingId());
            var unit = units.findInBuilding(query.buildingId(), query.unitId()).orElseThrow(UnitErrors::unitNotFound);
            return new UnitView(unit, floors.findInBuilding(query.buildingId(), unit.details().floorId())
                    .orElseThrow());
        });
    }
}
