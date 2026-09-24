package com.buildingos.building.unit.domain.repository;

import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.model.UnitBatch;
import java.util.List;
import java.util.UUID;

public interface UnitBatchRepository {
    /** Callers hold the building row lock and have validated every unit. */
    void insert(UnitBatch batch, List<Unit> units);
    List<Unit> findUnits(UUID buildingId, UUID batchId);
}
