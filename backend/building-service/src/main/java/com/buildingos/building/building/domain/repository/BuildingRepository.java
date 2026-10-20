package com.buildingos.building.building.domain.repository;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingStatus;
import java.util.List;
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
    /** Name ascending; null status = all, null {@code nameContains} = no filter (case-insensitive). */
    List<Building> findPage(BuildingStatus status, String nameContains, int offset, int limit);
    long count(BuildingStatus status, String nameContains);
}
