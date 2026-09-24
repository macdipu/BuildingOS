package com.buildingos.building.unit.application;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.membership.application.BuildingGrant;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.NotPermittedException;
import java.util.UUID;

/**
 * UO-D01 read scope: admins read every unit; an owner reads only units they currently own. Returns the owner filter
 * (null = unrestricted).
 */
public final class UnitReadScope {
    private UnitReadScope() {}

    public static UUID ownerFilter(Actor actor, BuildingGrant grant) {
        if (grant.isAdmin()) {
            return null;
        }
        if (grant.roles().contains(BuildingRole.OWNER)) {
            return actor.userId();
        }
        throw new NotPermittedException();
    }
}
