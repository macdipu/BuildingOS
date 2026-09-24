package com.buildingos.building.membership.domain.repository;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.membership.domain.model.BuildingInvitation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingInvitationRepository {
    void insert(BuildingInvitation invitation);
    /** Callers hold the building row lock. */
    void update(BuildingInvitation invitation);
    Optional<BuildingInvitation> findById(UUID id);
    Optional<BuildingInvitation> findInBuilding(UUID buildingId, UUID invitationId);
    Optional<BuildingInvitation> findPending(UUID buildingId, ContactPhone phone, BuildingRole role);
    List<BuildingInvitation> findByBuilding(UUID buildingId, int page, int size);
    long countByBuilding(UUID buildingId);
    List<BuildingInvitation> findLiveForPhone(ContactPhone phone, Instant now);
}
