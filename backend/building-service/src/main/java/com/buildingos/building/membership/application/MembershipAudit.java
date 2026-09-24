package com.buildingos.building.membership.application;

import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.membership.domain.model.BuildingInvitation;
import java.util.Map;

/** Permitted audit fields: state only, never phone numbers or identity tokens. */
public final class MembershipAudit {
    public static final String INVITATION = "INVITATION";
    public static final String MEMBERSHIP = "MEMBERSHIP";

    private MembershipAudit() {}

    public static Map<String, String> of(BuildingInvitation i) {
        return Map.of("status", i.status().name(), "role", i.role().name(), "expiresAt", i.expiresAt().toString(),
                "version", Long.toString(i.version()));
    }

    public static Map<String, String> of(BuildingMembership m) {
        return Map.of("status", m.status().name(), "role", m.role().name(), "userId", m.userId().toString(),
                "version", Long.toString(m.version()));
    }
}
