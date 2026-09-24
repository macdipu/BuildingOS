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
import java.util.stream.Collectors;

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

    /** Protected read for any current member or platform admin (FOR SHARE). */
    public BuildingGrant requireMemberToRead(Actor actor, UUID buildingId) {
        return grant(actor, buildings.findByIdForShare(buildingId).orElseThrow(() -> BuildingErrors.notFound(buildingId)));
    }

    /** Protected read: FOR SHARE lock, platform admin or active building admin. */
    public BuildingGrant requireAdminToRead(Actor actor, UUID buildingId) {
        return requireAdmin(requireMemberToRead(actor, buildingId));
    }

    /** Building-scoped write: FOR UPDATE lock, admin role, and SUSPENDED buildings are read-only. */
    public BuildingGrant requireAdminToWrite(Actor actor, UUID buildingId) {
        var building = buildings.findByIdForUpdate(buildingId).orElseThrow(() -> BuildingErrors.notFound(buildingId));
        var grant = requireAdmin(grant(actor, building));
        requireWritable(building);
        return grant;
    }

    public static void requireWritable(Building building) {
        if (building.status() == BuildingStatus.SUSPENDED) {
            throw MembershipErrors.buildingReadOnly();
        }
    }

    private BuildingGrant grant(Actor actor, Building building) {
        var roles = memberships.findActive(building.id(), actor.userId()).stream().map(BuildingMembership::role)
                .collect(Collectors.toSet());
        if (roles.isEmpty() && !actor.isPlatformAdmin()) {
            throw BuildingErrors.notFound(building.id());
        }
        return new BuildingGrant(building, roles, actor.isPlatformAdmin());
    }

    private static BuildingGrant requireAdmin(BuildingGrant grant) {
        if (!grant.isAdmin()) {
            throw new NotPermittedException();
        }
        return grant;
    }
}
