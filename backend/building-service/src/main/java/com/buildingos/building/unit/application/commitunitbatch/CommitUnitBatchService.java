package com.buildingos.building.unit.application.commitunitbatch;

import com.buildingos.building.unit.application.UnitAudit;
import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.unit.application.batch.BatchLimits;
import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.application.batch.BatchRowResult;
import com.buildingos.building.unit.application.batch.UnitBatchValidator;
import com.buildingos.building.membership.application.MembershipErrors;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.model.RecordedOperation;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.OperationRepository;
import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.model.UnitBatch;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitBatchRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * All-or-nothing batch creation (UO-04): revalidates every submitted row under the building lock; any invalid or
 * conflicting row rejects the whole batch. The operationId makes a retried commit return the same batch.
 */
public final class CommitUnitBatchService implements CommitUnitBatchUseCase {
    static final String ACTION = "COMMIT_UNIT_BATCH";

    private final BuildingAccess access;
    private final UnitBatchValidator validator;
    private final FloorRepository floors;
    private final UnitBatchRepository batches;
    private final OperationRepository operations;
    private final AuditRepository audit;
    private final BatchLimits limits;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CommitUnitBatchService(BuildingAccess access, UnitBatchValidator validator, FloorRepository floors,
            UnitBatchRepository batches, OperationRepository operations, AuditRepository audit, BatchLimits limits,
            UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.validator = validator;
        this.floors = floors;
        this.batches = batches;
        this.operations = operations;
        this.audit = audit;
        this.limits = limits;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public CommitResult execute(Actor actor, CommitUnitBatchCommand command) {
        if (command.operationId() == null) {
            throw new IllegalArgumentException("operationId is required");
        }
        String fingerprint = fingerprint(command.rows());
        return unitOfWork.inTransaction(() -> {
            var grant = access.requireAdminToWrite(actor, command.buildingId());
            var replay = operations.find(actor.userId(), ACTION, command.operationId());
            if (replay.isPresent()) {
                if (!replay.get().requestFingerprint().equals(fingerprint)
                        || !replay.get().buildingId().equals(command.buildingId())) {
                    throw MembershipErrors.idempotencyConflict();
                }
                var batchId = replay.get().resultEntityId();
                var units = batches.findUnits(command.buildingId(), batchId);
                return new CommitResult.Committed(new UnitBatch(batchId, command.buildingId(), actor.userId(),
                        units.size(), replay.get().createdAt()), views(command.buildingId(), units), true);
            }
            String reason = grant.auditReason(command.reason(), "Unit batch committed");
            var preview = validator.validate(command.buildingId(), command.rows(), limits);
            if (!preview.valid()) {
                return new CommitResult.Rejected(preview);
            }
            var now = clock.instant();
            var batch = UnitBatch.create(command.buildingId(), actor.userId(), preview.rows().size(), now);
            List<Unit> created = preview.rows().stream().map(BatchRowResult::details)
                    .map(d -> Unit.create(command.buildingId(), d, now)).toList();
            batches.insert(batch, created);
            for (var unit : created) {
                audit.append(AuditEntry.of(unit.buildingId(), actor.userId(), "UNIT_CREATED", UnitAudit.UNIT,
                        unit.id(), reason, Map.of(), UnitAudit.of(unit), now));
            }
            audit.append(AuditEntry.of(batch.buildingId(), actor.userId(), "UNIT_BATCH_COMMITTED", "UNIT_BATCH",
                    batch.id(), reason, Map.of(), Map.of("rowCount", Integer.toString(batch.rowCount())), now));
            operations.insert(new RecordedOperation(actor.userId(), ACTION, command.operationId(),
                    command.buildingId(), fingerprint, batch.id(), 0, now));
            return new CommitResult.Committed(batch, views(command.buildingId(), created), false);
        });
    }

    private List<UnitView> views(UUID buildingId, List<Unit> units) {
        Map<UUID, Floor> byId = floors.findByBuilding(buildingId).stream()
                .collect(Collectors.toMap(Floor::id, Function.identity()));
        return units.stream().map(u -> new UnitView(u, byId.get(u.details().floorId()))).toList();
    }

    private static String fingerprint(List<BatchRowInput> rows) {
        String canonical = rows.stream().map(r -> String.join("\u001f", Objects.toString(r.number(), ""),
                        Objects.toString(r.floorId(), ""), Objects.toString(r.floorLabel(), ""),
                        Objects.toString(r.type(), ""), Objects.toString(r.areaSqft(), ""),
                        Objects.toString(r.bedrooms(), ""), Objects.toString(r.defaultMaintenanceRate(), ""),
                        Objects.toString(r.notes(), "")))
                .collect(Collectors.joining("\u001e"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((ACTION + "\u001d" + canonical).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
