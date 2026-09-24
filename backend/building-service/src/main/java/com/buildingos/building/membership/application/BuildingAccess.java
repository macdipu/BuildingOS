package com.buildingos.building.membership.application;

import com.buildingos.building.building.application.BuildingErrors;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.model.BuildingStatus;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.NotPermittedException;
import java.util.UUID;

/**
 * Authoritative building authorization (TECH-SPEC-F4 "Mutation transaction and authorization"). Must run inside the
 * caller's transaction: the building row lock serializes the check with membership revocation and lifecycle changes.
 * A building the caller has no current membership in is reported as missing; a member without the role gets 403.
 */
public final class BuildingAccess {
    private final BuildingRepository buildings;
    private final BuildingMembershipRepository memberships;

    public BuildingAccess(BuildingRepository buildings, BuildingMembershipRepository memberships) {
        this.buildings = buildings;
        this.memberships = memberships;
    }

    /** Protected read: FOR SHARE lock, platform admin or active building admin. */
    public Building requireAdminToRead(Actor actor, UUID buildingId) {
        var building = buildings.findByIdForShare(buildingId).orElseThrow(() -> BuildingErrors.notFound(buildingId));
        requireAdmin(actor, building);
        return building;
    }

    /** Building-scoped write: FOR UPDATE lock, admin role, and SUSPENDED buildings are read-only. */
    public Building requireAdminToWrite(Actor actor, UUID buildingId) {
        var building = buildings.findByIdForUpdate(buildingId).orElseThrow(() -> BuildingErrors.notFound(buildingId));
        requireAdmin(actor, building);
        requireWritable(building);
        return building;
    }

    public static void requireWritable(Building building) {
        if (building.status() == BuildingStatus.SUSPENDED) {
            throw MembershipErrors.buildingReadOnly();
        }
    }

    private void requireAdmin(Actor actor, Building building) {
        if (actor.isPlatformAdmin()) {
            return;
        }
        var current = memberships.findActive(building.id(), actor.userId());
        if (current.isEmpty()) {
            throw BuildingErrors.notFound(building.id());
        }
        if (current.stream().noneMatch(BuildingMembership::isActiveAdmin)) {
            throw new NotPermittedException();
        }
    }
}
