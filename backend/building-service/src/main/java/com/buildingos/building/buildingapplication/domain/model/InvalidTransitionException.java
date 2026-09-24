package com.buildingos.building.buildingapplication.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public final class InvalidTransitionException extends DomainRuleException {
    public InvalidTransitionException(Enum<?> from, String action) {
        super("INVALID_TRANSITION", Kind.CONFLICT, "Cannot " + action + " from status " + from.name());
    }
}
