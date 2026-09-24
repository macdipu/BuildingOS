package com.buildingos.building.unit.domain.repository;

import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.model.UnitType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository {
    void insert(Unit unit);
    /** Callers hold the building row lock. */
    void update(Unit unit);
    Optional<Unit> findInBuilding(UUID buildingId, UUID unitId);
    /** {@code floorId}/{@code type} null = any; ordered by normalized number. */
    List<Unit> findByBuilding(UUID buildingId, UUID floorId, UnitType type, int page, int size);
    long count(UUID buildingId, UUID floorId, UnitType type);
    long countByBuilding(UUID buildingId);
    boolean numberTaken(UUID buildingId, String normalizedNumber, UUID exceptUnitId);
}
