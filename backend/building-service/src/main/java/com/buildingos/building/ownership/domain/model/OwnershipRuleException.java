package com.buildingos.building.ownership.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public final class OwnershipRuleException extends DomainRuleException {
    public OwnershipRuleException(String code, Kind kind, String message) {
        super(code, kind, message);
    }

    public static OwnershipRuleException invalid(String code, String message) {
        return new OwnershipRuleException(code, Kind.INVALID, message);
    }

    public static OwnershipRuleException conflict(String code, String message) {
        return new OwnershipRuleException(code, Kind.CONFLICT, message);
    }
}
