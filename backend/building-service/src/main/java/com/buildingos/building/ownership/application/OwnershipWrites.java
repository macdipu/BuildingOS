package com.buildingos.building.ownership.application;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.membership.application.MembershipErrors;
import com.buildingos.building.ownership.domain.model.OwnershipChange;
import com.buildingos.building.ownership.domain.model.OwnershipPeriod;
import com.buildingos.building.ownership.domain.model.OwnershipRuleException;
import com.buildingos.building.ownership.domain.model.UnitOwnership;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.domain.model.RecordedOperation;
import com.buildingos.building.shared.domain.repository.OperationRepository;
import com.buildingos.building.unit.application.UnitErrors;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shared steps of ownership writes: today's Asia/Dhaka date (UO-D02), owners must be claimed OWNER members, unit
 * lock after the building lock, and actor/action/operationId replay. Callers run inside one transaction.
 */
public final class OwnershipWrites {
    private final OwnershipRepository ownership;
    private final BuildingMembershipRepository memberships;
    private final OperationRepository operations;
    private final Clock clock;
    private final ZoneId zone;

    public OwnershipWrites(OwnershipRepository ownership, BuildingMembershipRepository memberships,
            OperationRepository operations, Clock clock, ZoneId zone) {
        this.ownership = ownership;
        this.memberships = memberships;
        this.operations = operations;
        this.clock = clock;
        this.zone = zone;
    }

    public void requireToday(LocalDate effectiveDate) {
        if (effectiveDate == null) {
            throw OwnershipRuleException.invalid("EFFECTIVE_DATE_REQUIRED", "effectiveDate is required");
        }
        if (!effectiveDate.equals(LocalDate.now(clock.withZone(zone)))) {
            throw OwnershipRuleException.invalid("EFFECTIVE_DATE_NOT_TODAY",
                    "Ownership changes take effect today (" + zone + "); past or future dates are not supported");
        }
    }

    public void requireOwnerMember(UUID buildingId, UUID userId, String field) {
        if (userId == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        boolean member = memberships.findActive(buildingId, userId).stream()
                .anyMatch(m -> m.role() == BuildingRole.OWNER);
        if (!member) {
            throw OwnershipRuleException.conflict("OWNER_NOT_MEMBER",
                    "Ownership can be assigned only to someone who has claimed an owner invitation for this building");
        }
    }

    public UnitOwnership lockUnit(UUID buildingId, UUID unitId) {
        return ownership.lockUnit(buildingId, unitId).orElseThrow(UnitErrors::unitNotFound);
    }

    /** Present when this actor already used the operationId: same request → prior result, else conflict. */
    public Optional<OwnershipResult> replay(Actor actor, String action, UUID operationId, String fingerprint,
            UUID buildingId, UUID unitId) {
        if (operationId == null) {
            throw new IllegalArgumentException("operationId is required");
        }
        var prior = operations.find(actor.userId(), action, operationId);
        if (prior.isEmpty()) {
            return Optional.empty();
        }
        if (!prior.get().requestFingerprint().equals(fingerprint)) {
            throw MembershipErrors.idempotencyConflict();
        }
        var now = lockUnit(buildingId, unitId);
        return Optional.of(new OwnershipResult(unitId, prior.get().resultVersion(), now.open(), null, true));
    }

    public OwnershipResult record(Actor actor, String action, UUID operationId, String fingerprint,
            UnitOwnership before, OwnershipChange change) {
        ownership.apply(change);
        operations.insert(new RecordedOperation(actor.userId(), action, operationId, before.buildingId(), fingerprint,
                before.unitId(), change.revision(), clock.instant()));
        Set<UUID> closed = change.closed().stream().map(OwnershipPeriod::id).collect(Collectors.toSet());
        List<OwnershipPeriod> current = new ArrayList<>(before.open().stream()
                .filter(p -> !closed.contains(p.id())).toList());
        current.addAll(change.opened());
        return new OwnershipResult(before.unitId(), change.revision(), current, change.transfer(), false);
    }

    public static String optionalText(String value, int max, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        return trimmed;
    }
}
