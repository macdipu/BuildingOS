package com.buildingos.building.unit.application.updateunit;

import com.buildingos.building.unit.application.UnitAudit;
import com.buildingos.building.unit.application.UnitErrors;
import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import java.time.Clock;

public final class UpdateUnitService implements UpdateUnitUseCase {
    private final BuildingAccess access;
    private final FloorRepository floors;
    private final UnitRepository units;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UpdateUnitService(BuildingAccess access, FloorRepository floors, UnitRepository units,
            AuditRepository audit, UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.floors = floors;
        this.units = units;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public UnitView execute(Actor actor, UpdateUnitCommand command) {
        var details = command.input().details();
        if (command.expectedVersion() == null) {
            throw new IllegalArgumentException("expectedVersion is required");
        }
        return unitOfWork.inTransaction(() -> {
            var grant = access.requireAdminToWrite(actor, command.buildingId());
            String reason = grant.auditReason(command.reason(), "Unit updated");
            var current = units.findInBuilding(command.buildingId(), command.unitId())
                    .orElseThrow(UnitErrors::unitNotFound);
            var floor = floors.findInBuilding(command.buildingId(), details.floorId())
                    .orElseThrow(UnitErrors::floorNotFound);
            if (units.numberTaken(command.buildingId(), details.normalizedNumber(), current.id())) {
                throw UnitErrors.unitNumberTaken();
            }
            var now = clock.instant();
            var next = current.updated(details, command.expectedVersion(), now);
            units.update(next);
            audit.append(AuditEntry.of(next.buildingId(), actor.userId(), "UNIT_UPDATED", UnitAudit.UNIT, next.id(),
                    reason, UnitAudit.of(current), UnitAudit.of(next), now));
            return new UnitView(next, floor);
        });
    }
}
