package com.buildingos.building.membership.application.getbuildingcontext;

import com.buildingos.building.membership.application.BuildingGrant;
import com.buildingos.building.building.domain.model.BuildingStatus;

/** Caller-specific capabilities; the backend still re-authorizes every action (UO-01). */
public record BuildingContext(BuildingGrant grant) {
    public boolean readOnly() { return grant.building().status() == BuildingStatus.SUSPENDED; }
    public boolean canManageMembers() { return grant.isAdmin() && !readOnly(); }
    public boolean canManageUnits() { return grant.isAdmin() && !readOnly(); }
}
