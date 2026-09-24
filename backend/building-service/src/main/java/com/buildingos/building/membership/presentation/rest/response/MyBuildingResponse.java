package com.buildingos.building.membership.presentation.rest.response;

import com.buildingos.building.membership.application.listmybuildings.MyBuilding;
import java.util.List;
import java.util.UUID;

public record MyBuildingResponse(UUID id, String name, String address, String area, String district, String status,
        List<String> roles, long ownedUnitCount) {
    public static MyBuildingResponse of(MyBuilding mine) {
        var b = mine.building();
        return new MyBuildingResponse(b.id(), b.name(), b.address(), b.area(), b.district(), b.status().name(),
                mine.roles().stream().map(Enum::name).sorted().toList(), mine.ownedUnitCount());
    }
}
