package com.buildingos.building.building.domain.repository;

import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.model.BuildingRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingMembershipRepository {
    void insert(BuildingMembership membership);
    /** Callers hold the building row lock. */
    void update(BuildingMembership membership);
    List<BuildingMembership> findByBuilding(UUID buildingId);
    List<BuildingMembership> findByBuilding(UUID buildingId, int page, int size);
    long countByBuilding(UUID buildingId);
    Optional<BuildingMembership> findInBuilding(UUID buildingId, UUID membershipId);
    Optional<BuildingMembership> find(UUID buildingId, UUID userId, BuildingRole role);
    List<BuildingMembership> findActive(UUID buildingId, UUID userId);
    long countActiveAdmins(UUID buildingId);
}
