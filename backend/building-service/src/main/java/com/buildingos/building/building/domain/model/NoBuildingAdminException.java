package com.buildingos.building.building.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public final class NoBuildingAdminException extends DomainRuleException {
    public NoBuildingAdminException() {
        super("NO_BUILDING_ADMIN", Kind.CONFLICT, "Activation requires an active building admin");
    }
}
