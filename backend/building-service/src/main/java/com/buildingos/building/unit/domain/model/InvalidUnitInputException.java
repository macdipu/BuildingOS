package com.buildingos.building.unit.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

/** A rejected floor/unit field; {@code field} lets batch previews report row/field errors (UO-04). */
public final class InvalidUnitInputException extends DomainRuleException {
    private final String field;

    public InvalidUnitInputException(String code, String field, String message) {
        super(code, Kind.INVALID, message);
        this.field = field;
    }

    public String field() { return field; }
}
