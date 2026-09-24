package com.buildingos.building.unit.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

/** A whole batch or sheet refused before row validation (format, size, row count, formulas). */
public final class BatchRejectedException extends DomainRuleException {
    public BatchRejectedException(String code, Kind kind, String message) {
        super(code, kind, message);
    }

    public static BatchRejectedException invalid(String code, String message) {
        return new BatchRejectedException(code, Kind.INVALID, message);
    }
}
