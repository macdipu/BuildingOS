package com.buildingos.building.unit.application.createunit;

import com.buildingos.building.unit.application.UnitAudit;
import com.buildingos.building.unit.application.UnitErrors;
import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import java.time.Clock;
import java.util.Map;

/** Individual unit creation; the building lock serializes number uniqueness and activation checks (UO-03/09). */
public final class CreateUnitService implements CreateUnitUseCase {
    private final BuildingAccess access;
    private final FloorRepository floors;
    private final UnitRepository units;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CreateUnitService(BuildingAccess access, FloorRepository floors, UnitRepository units,
            AuditRepository audit, UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.floors = floors;
        this.units = units;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public UnitView execute(Actor actor, CreateUnitCommand command) {
        var details = command.input().details();
        return unitOfWork.inTransaction(() -> {
            var grant = access.requireAdminToWrite(actor, command.buildingId());
            String reason = grant.auditReason(command.reason(), "Unit created");
            var floor = floors.findInBuilding(command.buildingId(), details.floorId())
                    .orElseThrow(UnitErrors::floorNotFound);
            if (units.numberTaken(command.buildingId(), details.normalizedNumber(), null)) {
                throw UnitErrors.unitNumberTaken();
            }
            var now = clock.instant();
            var unit = Unit.create(command.buildingId(), details, now);
            units.insert(unit);
            audit.append(AuditEntry.of(unit.buildingId(), actor.userId(), "UNIT_CREATED", UnitAudit.UNIT, unit.id(),
                    reason, Map.of(), UnitAudit.of(unit), now));
            return new UnitView(unit, floor);
        });
    }
}
