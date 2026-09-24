package com.buildingos.building.unit.application.commitunitbatch;

import com.buildingos.building.unit.application.batch.BatchRowInput;
import java.util.List;
import java.util.UUID;

public record CommitUnitBatchCommand(UUID buildingId, List<BatchRowInput> rows, UUID operationId, String reason) {
    public CommitUnitBatchCommand {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }
}
