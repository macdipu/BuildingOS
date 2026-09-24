package com.buildingos.building.ownership.presentation.rest.response;

import com.buildingos.building.ownership.domain.model.OwnershipHistory;
import java.util.List;

public record OwnershipHistoryResponse(long revision, List<PeriodResponse> periods, List<TransferResponse> transfers) {
    public static OwnershipHistoryResponse of(OwnershipHistory h) {
        return new OwnershipHistoryResponse(h.revision(), h.periods().stream().map(PeriodResponse::of).toList(),
                h.transfers().stream().map(TransferResponse::of).toList());
    }
}
