package com.buildingos.building.unit.application.getunit;

import com.buildingos.building.unit.application.UnitErrors;
import com.buildingos.building.unit.application.UnitReadScope;
import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;

/** Admins read any unit; an owner reads only a unit they currently own, others look missing (UO-08). */
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
            var owner = UnitReadScope.ownerFilter(actor, access.requireMemberToRead(actor, query.buildingId()));
            var unit = units.findInBuilding(query.buildingId(), query.unitId())
                    .filter(u -> owner == null || units.currentlyOwnedBy(u.id(), owner))
                    .orElseThrow(UnitErrors::unitNotFound);
            return new UnitView(unit, floors.findInBuilding(query.buildingId(), unit.details().floorId())
                    .orElseThrow());
        });
    }
}
