package com.buildingos.building.building.application;

import com.buildingos.building.shared.application.BusinessRuleException;
import java.util.UUID;

public final class BuildingErrors {
    private BuildingErrors() {}

    public static BusinessRuleException notFound(UUID id) {
        return BusinessRuleException.notFound("BUILDING_NOT_FOUND", "Building " + id + " does not exist");
    }
}
