package com.buildingos.building.unit.application;

import com.buildingos.building.shared.application.BusinessRuleException;

public final class UnitErrors {
    private UnitErrors() {}

    public static BusinessRuleException floorNotFound() {
        return BusinessRuleException.notFound("FLOOR_NOT_FOUND", "Floor does not exist in this building");
    }

    public static BusinessRuleException unitNotFound() {
        return BusinessRuleException.notFound("UNIT_NOT_FOUND", "Unit does not exist in this building");
    }

    public static BusinessRuleException unitNumberTaken() {
        return BusinessRuleException.conflict("UNIT_NUMBER_TAKEN", "Unit number already exists in this building");
    }

    public static BusinessRuleException floorLabelTaken() {
        return BusinessRuleException.conflict("FLOOR_LABEL_TAKEN", "Floor label already exists in this building");
    }
}
