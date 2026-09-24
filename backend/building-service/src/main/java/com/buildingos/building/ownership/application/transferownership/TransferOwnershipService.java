package com.buildingos.building.ownership.application.transferownership;

import com.buildingos.building.ownership.application.OwnershipResult;
import com.buildingos.building.ownership.application.OwnershipWrites;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.ownership.domain.model.OwnershipTransfer;
import com.buildingos.building.ownership.domain.model.Share;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Fingerprints;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.model.OutboxEvent;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Moves a share between owners (UO-07): closes affected periods, keeps history, and commits the audit row and the
 * {@code ownership.transferred} v1 outbox event in the same transaction (UO-10).
 */
public final class TransferOwnershipService implements TransferOwnershipUseCase {
    static final String ACTION = "TRANSFER_OWNERSHIP";
    public static final String EVENT_TYPE = "ownership.transferred";
    private static final int MAX_REFERENCE = 200;

    private final BuildingAccess access;
    private final OwnershipWrites writes;
    private final AuditRepository audit;
    private final OutboxRepository outbox;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public TransferOwnershipService(BuildingAccess access, OwnershipWrites writes, AuditRepository audit,
            OutboxRepository outbox, UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.writes = writes;
        this.audit = audit;
        this.outbox = outbox;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public OwnershipResult execute(Actor actor, TransferOwnershipCommand c) {
        var share = Share.of(c.share());
        String reason = BuildingApplication.reason(c.reason(), "reason");
        String reference = OwnershipWrites.optionalText(c.reference(), MAX_REFERENCE, "reference");
        if (c.expectedVersion() == null) {
            throw new IllegalArgumentException("expectedVersion is required");
        }
        if (c.sourceOwnerUserId() == null) {
            throw new IllegalArgumentException("sourceOwnerUserId is required");
        }
        String fingerprint = Fingerprints.of(ACTION, c.buildingId(), c.unitId(), c.sourceOwnerUserId(),
                c.recipientUserId(), share.percent(), c.effectiveDate(), reference, reason, c.expectedVersion());
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToWrite(actor, c.buildingId());
            var replay = writes.replay(actor, ACTION, c.operationId(), fingerprint, c.buildingId(), c.unitId());
            if (replay.isPresent()) {
                return replay.get();
            }
            writes.requireToday(c.effectiveDate());
            writes.requireOwnerMember(c.buildingId(), c.recipientUserId(), "recipientUserId");
            var before = writes.lockUnit(c.buildingId(), c.unitId());
            var now = clock.instant();
            var change = before.transfer(c.sourceOwnerUserId(), c.recipientUserId(), share, c.effectiveDate(),
                    actor.userId(), reason, reference, c.expectedVersion(), now);
            var result = writes.record(actor, ACTION, c.operationId(), fingerprint, before, change);
            var transfer = change.transfer();
            audit.append(AuditEntry.of(c.buildingId(), actor.userId(), "OWNERSHIP_TRANSFERRED", "OWNERSHIP_TRANSFER",
                    transfer.id(), reason, Map.of("sourceShare", before.openFor(c.sourceOwnerUserId()).orElseThrow()
                            .share().percent().toPlainString()), fields(transfer), now));
            outbox.append(new OutboxEvent(UUID.randomUUID(), EVENT_TYPE, 1, c.buildingId(), "UNIT", c.unitId(),
                    transfer.revision(), data(transfer), now));
            return result;
        });
    }

    private static Map<String, String> fields(OwnershipTransfer t) {
        return Map.of("unitId", t.unitId().toString(), "sourceOwnerUserId", t.sourceOwnerUserId().toString(),
                "recipientUserId", t.recipientUserId().toString(), "share", t.share().percent().toPlainString(),
                "revision", Long.toString(t.revision()));
    }

    /** contracts/kafka/ownership-transferred.v1: identifiers and share only; no contact details or documents. */
    private static Map<String, Object> data(OwnershipTransfer t) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transferId", t.id().toString());
        data.put("buildingId", t.buildingId().toString());
        data.put("unitId", t.unitId().toString());
        data.put("sourceOwnerUserId", t.sourceOwnerUserId().toString());
        data.put("recipientUserId", t.recipientUserId().toString());
        data.put("share", t.share().percent());
        data.put("effectiveDate", t.effectiveDate().toString());
        data.put("effectiveAt", t.effectiveAt().toString());
        data.put("revision", t.revision());
        return data;
    }
}
