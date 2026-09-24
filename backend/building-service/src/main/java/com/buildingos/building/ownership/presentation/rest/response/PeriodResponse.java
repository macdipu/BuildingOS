package com.buildingos.building.ownership.presentation.rest.response;

import com.buildingos.building.ownership.domain.model.OwnershipPeriod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PeriodResponse(UUID id, UUID ownerUserId, BigDecimal share, Instant startAt, long startRevision,
        Instant endAt, Long endRevision, String notes) {
    public static PeriodResponse of(OwnershipPeriod p) {
        return new PeriodResponse(p.id(), p.ownerUserId(), p.share().percent(), p.startAt(), p.startRevision(),
                p.endAt(), p.endRevision(), p.notes());
    }
}
