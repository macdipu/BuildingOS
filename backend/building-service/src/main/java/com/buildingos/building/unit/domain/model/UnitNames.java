package com.buildingos.building.unit.domain.model;

import java.util.Locale;

/** UO-D03: trim the edges, compare case-insensitively within a building, keep the display spelling. */
public final class UnitNames {
    private UnitNames() {}

    public static String display(String raw, int maxLength, String field, String code) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidUnitInputException(code + "_REQUIRED", field, field + " is required");
        }
        String trimmed = raw.strip();
        if (trimmed.length() > maxLength) {
            throw new InvalidUnitInputException(code + "_TOO_LONG", field,
                    field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    public static String normalized(String display) {
        return display.toUpperCase(Locale.ROOT);
    }
}
