package com.buildingos.building.shared.presentation.rest;

import java.util.Locale;

/** Parses client enum strings so an unknown value is a 400, not a framework error. */
public final class Enums {
    private Enums() {}

    /** Null or blank stays null (optional or not yet filled draft field). */
    public static <E extends Enum<E>> E parseOptional(Class<E> type, String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(field + " has an unknown value: " + value);
        }
    }
}
