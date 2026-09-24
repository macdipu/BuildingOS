package com.buildingos.building.building.domain.repository;

import com.buildingos.building.building.domain.model.BuildingMembership;
import java.util.List;
import java.util.UUID;

public interface BuildingMembershipRepository {
    void insert(BuildingMembership membership);
    List<BuildingMembership> findByBuilding(UUID buildingId);
    long countActiveAdmins(UUID buildingId);
}
