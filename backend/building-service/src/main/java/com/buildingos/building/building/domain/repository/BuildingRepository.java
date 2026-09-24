package com.buildingos.building.building.domain.repository;

import com.buildingos.building.building.domain.model.Building;
import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository {
    void insert(Building building);
    /** Callers hold the row lock from {@link #findByIdForUpdate}. */
    void update(Building building);
    Optional<Building> findById(UUID id);
    Optional<Building> findByIdForUpdate(UUID id);
    /** Blocks concurrent lifecycle and membership writes while a protected read runs. */
    Optional<Building> findByIdForShare(UUID id);
    Optional<Building> findByApplication(UUID applicationId);
}
