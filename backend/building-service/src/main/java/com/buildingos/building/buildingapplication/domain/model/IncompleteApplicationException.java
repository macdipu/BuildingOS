package com.buildingos.building.buildingapplication.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;
import java.util.List;

public final class IncompleteApplicationException extends DomainRuleException {
    private final List<String> missingFields;

    public IncompleteApplicationException(List<String> missingFields) {
        super("APPLICATION_INCOMPLETE", Kind.INVALID, "Missing required fields: " + String.join(", ", missingFields));
        this.missingFields = List.copyOf(missingFields);
    }

    public List<String> missingFields() { return missingFields; }
}
