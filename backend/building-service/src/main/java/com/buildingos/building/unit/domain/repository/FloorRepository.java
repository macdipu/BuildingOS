package com.buildingos.building.unit.domain.repository;

import com.buildingos.building.unit.domain.model.Floor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FloorRepository {
    void insert(Floor floor);
    /** Callers hold the building row lock. */
    void update(Floor floor);
    Optional<Floor> findInBuilding(UUID buildingId, UUID floorId);
    List<Floor> findByBuilding(UUID buildingId);
    List<Floor> findByBuilding(UUID buildingId, int page, int size);
    long countByBuilding(UUID buildingId);
    boolean labelTaken(UUID buildingId, String normalizedLabel, UUID exceptFloorId);
}
