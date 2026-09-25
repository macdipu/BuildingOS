package com.buildingos.building.unit.domain.repository;

import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.model.UnitSearch;
import com.buildingos.building.unit.domain.model.UnitType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UnitRepository {
    void insert(Unit unit);
    /** Callers hold the building row lock. */
    void update(Unit unit);
    Optional<Unit> findInBuilding(UUID buildingId, UUID unitId);
    List<Unit> search(UUID buildingId, UnitSearch search, int page, int size);
    long count(UUID buildingId, UnitSearch search);

    /** {@code floorId}/{@code type}/{@code ownerUserId} null = any; ordered by normalized number. */
    default List<Unit> findByBuilding(UUID buildingId, UUID floorId, UnitType type, UUID ownerUserId, int page,
            int size) {
        return search(buildingId, UnitSearch.of(floorId, type, ownerUserId), page, size);
    }

    default long count(UUID buildingId, UUID floorId, UnitType type, UUID ownerUserId) {
        return count(buildingId, UnitSearch.of(floorId, type, ownerUserId));
    }
    /** True when {@code userId} holds a current (open) allocation on the unit. */
    boolean currentlyOwnedBy(UUID unitId, UUID userId);
    long countByBuilding(UUID buildingId);
    boolean numberTaken(UUID buildingId, String normalizedNumber, UUID exceptUnitId);
    /** The subset of {@code normalizedNumbers} already used in the building. */
    Set<String> takenNumbers(UUID buildingId, Collection<String> normalizedNumbers);
}
