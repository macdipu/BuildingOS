package com.buildingos.building.unit.application.createfloor;

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
import java.util.Map;

public final class CreateFloorService implements CreateFloorUseCase {
    private final BuildingAccess access;
    private final FloorRepository floors;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CreateFloorService(BuildingAccess access, FloorRepository floors, AuditRepository audit,
            UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.floors = floors;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Floor execute(Actor actor, CreateFloorCommand command) {
        var details = command.input().details();
        return unitOfWork.inTransaction(() -> {
            var grant = access.requireAdminToWrite(actor, command.buildingId());
            String reason = grant.auditReason(command.reason(), "Floor created");
            if (floors.labelTaken(command.buildingId(), details.normalizedLabel(), null)) {
                throw UnitErrors.floorLabelTaken();
            }
            var now = clock.instant();
            var floor = Floor.create(command.buildingId(), details, now);
            floors.insert(floor);
            audit.append(AuditEntry.of(floor.buildingId(), actor.userId(), "FLOOR_CREATED", UnitAudit.FLOOR,
                    floor.id(), reason, Map.of(), UnitAudit.of(floor), now));
            return floor;
        });
    }
}
