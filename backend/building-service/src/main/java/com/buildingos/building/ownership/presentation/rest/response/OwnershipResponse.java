package com.buildingos.building.ownership.presentation.rest.response;

import com.buildingos.building.ownership.application.OwnershipResult;
import java.util.List;
import java.util.UUID;

/** {@code revision} is the unit ownership revision to send as the next {@code expectedVersion}. */
public record OwnershipResponse(UUID unitId, long revision, boolean replayed, List<PeriodResponse> current,
        TransferResponse transfer) {
    public static OwnershipResponse of(OwnershipResult r) {
        return new OwnershipResponse(r.unitId(), r.revision(), r.replayed(),
                r.current().stream().map(PeriodResponse::of).toList(), TransferResponse.of(r.transfer()));
    }
}
