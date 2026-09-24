package com.buildingos.building.ownership.presentation.rest.response;

import com.buildingos.building.ownership.domain.model.UnitOwnership;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** {@code revision} is the next {@code expectedVersion}; {@code allocated} is the visible allocations' total. */
public record CurrentOwnershipResponse(UUID unitId, long revision, BigDecimal allocated, List<PeriodResponse> current) {
    public static CurrentOwnershipResponse of(UnitOwnership o) {
        return new CurrentOwnershipResponse(o.unitId(), o.revision(), o.allocated(),
                o.open().stream().map(PeriodResponse::of).toList());
    }
}
