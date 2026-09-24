package com.buildingos.building.unit.application.updatefloor;

import com.buildingos.building.unit.application.UnitAudit;
import com.buildingos.building.unit.application.UnitErrors;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import java.time.Clock;

public final class UpdateFloorService implements UpdateFloorUseCase {
    private final BuildingAccess access;
    private final FloorRepository floors;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UpdateFloorService(BuildingAccess access, FloorRepository floors, AuditRepository audit,
            UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.floors = floors;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Floor execute(Actor actor, UpdateFloorCommand command) {
        var details = command.input().details();
        if (command.expectedVersion() == null) {
            throw new IllegalArgumentException("expectedVersion is required");
        }
        return unitOfWork.inTransaction(() -> {
            var grant = access.requireAdminToWrite(actor, command.buildingId());
            String reason = grant.auditReason(command.reason(), "Floor updated");
            var current = floors.findInBuilding(command.buildingId(), command.floorId())
                    .orElseThrow(UnitErrors::floorNotFound);
            if (floors.labelTaken(command.buildingId(), details.normalizedLabel(), current.id())) {
                throw UnitErrors.floorLabelTaken();
            }
            var now = clock.instant();
            var next = current.updated(details, command.expectedVersion(), now);
            floors.update(next);
            audit.append(AuditEntry.of(next.buildingId(), actor.userId(), "FLOOR_UPDATED", UnitAudit.FLOOR, next.id(),
                    reason, UnitAudit.of(current), UnitAudit.of(next), now));
            return next;
        });
    }
}
