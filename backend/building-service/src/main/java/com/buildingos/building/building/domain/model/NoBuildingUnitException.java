package com.buildingos.building.building.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public final class NoBuildingUnitException extends DomainRuleException {
    public NoBuildingUnitException() {
        super("NO_BUILDING_UNIT", Kind.CONFLICT, "Activation requires at least one unit");
    }
}
