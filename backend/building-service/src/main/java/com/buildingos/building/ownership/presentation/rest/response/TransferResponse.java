package com.buildingos.building.ownership.presentation.rest.response;

import com.buildingos.building.ownership.domain.model.OwnershipTransfer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransferResponse(UUID id, UUID sourceOwnerUserId, UUID recipientUserId, BigDecimal share,
        LocalDate effectiveDate, Instant effectiveAt, long revision, UUID actorUserId, String reason,
        String reference) {
    public static TransferResponse of(OwnershipTransfer t) {
        return t == null ? null : new TransferResponse(t.id(), t.sourceOwnerUserId(), t.recipientUserId(),
                t.share().percent(), t.effectiveDate(), t.effectiveAt(), t.revision(), t.actorUserId(), t.reason(),
                t.reference());
    }
}
