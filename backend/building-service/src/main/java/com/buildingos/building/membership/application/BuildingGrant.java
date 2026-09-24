package com.buildingos.building.membership.application;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import java.util.Set;

/** The caller's current authority over one building, resolved under the building row lock. */
public record BuildingGrant(Building building, Set<BuildingRole> roles, boolean platformAdmin) {
    public BuildingGrant {
        roles = Set.copyOf(roles);
    }

    public boolean isAdmin() {
        return platformAdmin || roles.contains(BuildingRole.BUILDING_ADMIN);
    }

    /**
     * Audit reason for a building write. Platform admins acting without a building-admin membership must state one
     * (TECH-SPEC-F4 "Platform writes require audit reasons"); building admins may rely on the action's description.
     */
    public String auditReason(String requested, String fallback) {
        if (requested != null && !requested.isBlank()) {
            return BuildingApplication.reason(requested, "reason");
        }
        if (!roles.contains(BuildingRole.BUILDING_ADMIN)) {
            throw new IllegalArgumentException("reason is required for platform administrators");
        }
        return fallback;
    }
}
