package com.buildingos.building.ownership.application.assignownership;

import com.buildingos.building.ownership.application.OwnershipResult;
import com.buildingos.building.ownership.application.OwnershipWrites;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.ownership.domain.model.Share;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Fingerprints;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.time.Clock;
import java.util.Map;

/** Allocates a share of a unit to a claimed owner; the unit total stays at or below 100% (UO-06). */
public final class AssignOwnershipService implements AssignOwnershipUseCase {
    static final String ACTION = "ASSIGN_OWNERSHIP";
    private static final int MAX_NOTES = 1000;

    private final BuildingAccess access;
    private final OwnershipWrites writes;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public AssignOwnershipService(BuildingAccess access, OwnershipWrites writes, AuditRepository audit,
            UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.writes = writes;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public OwnershipResult execute(Actor actor, AssignOwnershipCommand c) {
        var share = Share.of(c.share());
        String notes = OwnershipWrites.optionalText(c.notes(), MAX_NOTES, "notes");
        if (c.expectedVersion() == null) {
            throw new IllegalArgumentException("expectedVersion is required");
        }
        String fingerprint = Fingerprints.of(ACTION, c.buildingId(), c.unitId(), c.ownerUserId(), share.percent(),
                c.effectiveDate(), notes, c.reason(), c.expectedVersion());
        return unitOfWork.inTransaction(() -> {
            var grant = access.requireAdminToWrite(actor, c.buildingId());
            var replay = writes.replay(actor, ACTION, c.operationId(), fingerprint, c.buildingId(), c.unitId());
            if (replay.isPresent()) {
                return replay.get();
            }
            String reason = grant.auditReason(c.reason(), "Ownership assigned");
            writes.requireToday(c.effectiveDate());
            writes.requireOwnerMember(c.buildingId(), c.ownerUserId(), "ownerUserId");
            var before = writes.lockUnit(c.buildingId(), c.unitId());
            var now = clock.instant();
            var change = before.assign(c.ownerUserId(), share, notes, actor.userId(), c.expectedVersion(), now);
            var result = writes.record(actor, ACTION, c.operationId(), fingerprint, before, change);
            var period = change.opened().get(0);
            audit.append(AuditEntry.of(c.buildingId(), actor.userId(), "OWNERSHIP_ASSIGNED", "OWNERSHIP_PERIOD",
                    period.id(), reason, Map.of(), Map.of("unitId", c.unitId().toString(),
                            "ownerUserId", c.ownerUserId().toString(), "share", share.percent().toPlainString(),
                            "revision", Long.toString(change.revision())), now));
            return result;
        });
    }
}
