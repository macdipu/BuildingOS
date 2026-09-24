package com.buildingos.building.unit.presentation.rest.response;

import com.buildingos.building.unit.application.commitunitbatch.CommitResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BatchCommitResponse(UUID batchId, int unitCount, boolean replayed, Instant createdAt,
        List<UnitResponse> units) {
    public static BatchCommitResponse of(CommitResult.Committed committed) {
        var batch = committed.batch();
        return new BatchCommitResponse(batch.id(), committed.units().size(), committed.replayed(), batch.createdAt(),
                committed.units().stream().map(UnitResponse::of).toList());
    }
}
