package com.buildingos.building.building.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public final class MembershipStateException extends DomainRuleException {
    public MembershipStateException(String code, String message) {
        super(code, Kind.CONFLICT, message);
    }
}
