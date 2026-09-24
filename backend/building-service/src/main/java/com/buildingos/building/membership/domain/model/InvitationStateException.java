package com.buildingos.building.membership.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public final class InvitationStateException extends DomainRuleException {
    public InvitationStateException(String code, String message) {
        super(code, Kind.CONFLICT, message);
    }
}
