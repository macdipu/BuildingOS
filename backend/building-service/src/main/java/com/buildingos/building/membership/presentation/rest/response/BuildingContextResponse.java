package com.buildingos.building.membership.presentation.rest.response;

import com.buildingos.building.membership.application.getbuildingcontext.BuildingContext;
import java.util.List;
import java.util.UUID;

public record BuildingContextResponse(UUID id, String name, String buildingType, String address, String area,
        String district, String postalCode, String status, List<String> roles, boolean platformAdmin,
        Capabilities capabilities) {
    public record Capabilities(boolean readOnly, boolean manageMembers, boolean manageUnits) {}

    public static BuildingContextResponse of(BuildingContext context) {
        var grant = context.grant();
        var b = grant.building();
        return new BuildingContextResponse(b.id(), b.name(), b.type().name(), b.address(), b.area(), b.district(),
                b.postalCode(), b.status().name(), grant.roles().stream().map(Enum::name).sorted().toList(),
                grant.platformAdmin(),
                new Capabilities(context.readOnly(), context.canManageMembers(), context.canManageUnits()));
    }
}
