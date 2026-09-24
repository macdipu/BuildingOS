package com.buildingos.building.unit.application.listunits;

import com.buildingos.building.unit.application.UnitReadScope;
import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Admins list every unit; an owner lists only units they currently own (UO-D01, UO-08). */
public final class ListUnitsService implements ListUnitsUseCase {
    private final BuildingAccess access;
    private final FloorRepository floors;
    private final UnitRepository units;
    private final UnitOfWork unitOfWork;

    public ListUnitsService(BuildingAccess access, FloorRepository floors, UnitRepository units,
            UnitOfWork unitOfWork) {
        this.access = access;
        this.floors = floors;
        this.units = units;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Page<UnitView> execute(Actor actor, ListUnitsQuery query) {
        Page.validate(query.page(), query.size());
        return unitOfWork.inTransaction(() -> {
            var owner = UnitReadScope.ownerFilter(actor, access.requireMemberToRead(actor, query.buildingId()));
            Map<UUID, Floor> byId = floors.findByBuilding(query.buildingId()).stream()
                    .collect(Collectors.toMap(Floor::id, Function.identity()));
            var items = units.findByBuilding(query.buildingId(), query.floorId(), query.type(), owner, query.page(),
                    query.size()).stream().map(u -> new UnitView(u, byId.get(u.details().floorId()))).toList();
            return new Page<>(items, query.page(), query.size(),
                    units.count(query.buildingId(), query.floorId(), query.type(), owner));
        });
    }
}
