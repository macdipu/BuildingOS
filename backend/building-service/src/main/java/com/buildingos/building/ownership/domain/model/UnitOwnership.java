package com.buildingos.building.ownership.domain.model;

import com.buildingos.building.shared.domain.model.StaleVersionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Current allocations of one unit at its ordered ownership revision (TECH-SPEC-F4 "Ownership model"). Total current
 * share never exceeds 100; zero/unowned balance is valid. Callers hold the building and unit locks.
 */
public record UnitOwnership(UUID buildingId, UUID unitId, long revision, List<OwnershipPeriod> open) {
    public UnitOwnership {
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(unitId, "unitId");
        open = List.copyOf(open);
    }

    public BigDecimal allocated() {
        return open.stream().map(p -> p.share().percent()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Optional<OwnershipPeriod> openFor(UUID owner) {
        return open.stream().filter(p -> p.ownerUserId().equals(owner)).findFirst();
    }

    public OwnershipChange assign(UUID owner, Share share, String notes, UUID actor, long expectedRevision,
            Instant at) {
        requireRevision(expectedRevision);
        if (openFor(owner).isPresent()) {
            throw OwnershipRuleException.conflict("ALREADY_OWNER",
                    "This person already owns part of the unit; use a transfer to change the share");
        }
        if (allocated().add(share.percent()).compareTo(Share.HUNDRED) > 0) {
            throw OwnershipRuleException.conflict("SHARE_EXCEEDED", "Total ownership would exceed 100%");
        }
        long next = revision + 1;
        return new OwnershipChange(revision, next, List.of(),
                List.of(OwnershipPeriod.open(buildingId, unitId, owner, share, at, next, notes, actor)), null);
    }

    public OwnershipChange transfer(UUID source, UUID recipient, Share share, LocalDate effectiveDate, UUID actor,
            String reason, String reference, long expectedRevision, Instant at) {
        requireRevision(expectedRevision);
        if (source.equals(recipient)) {
            throw OwnershipRuleException.invalid("SAME_OWNER", "Source and recipient must differ");
        }
        var from = openFor(source).orElseThrow(() -> OwnershipRuleException.conflict("SOURCE_NOT_OWNER",
                "The source does not currently own part of the unit"));
        if (from.share().compareTo(share) < 0) {
            throw OwnershipRuleException.conflict("INSUFFICIENT_SOURCE_SHARE",
                    "The source owns less than the transferred share");
        }
        long next = revision + 1;
        List<OwnershipPeriod> closed = new ArrayList<>();
        List<OwnershipPeriod> opened = new ArrayList<>();
        closed.add(from.closed(at, next));
        var remainder = from.share().minusOrNull(share);
        if (remainder != null) {
            opened.add(OwnershipPeriod.open(buildingId, unitId, source, remainder, at, next, from.notes(), actor));
        }
        var to = openFor(recipient);
        Share received = share;
        String notes = null;
        if (to.isPresent()) {
            closed.add(to.get().closed(at, next));
            received = to.get().share().plus(share);
            notes = to.get().notes();
        }
        opened.add(OwnershipPeriod.open(buildingId, unitId, recipient, received, at, next, notes, actor));
        var transfer = new OwnershipTransfer(UUID.randomUUID(), buildingId, unitId, source, recipient, share,
                effectiveDate, at, next, actor, reason, reference);
        return new OwnershipChange(revision, next, closed, opened, transfer);
    }

    private void requireRevision(long expected) {
        if (expected != revision) {
            throw new StaleVersionException("Unit ownership");
        }
    }
}
