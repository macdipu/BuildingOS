package com.buildingos.backoffice.shared.presentation.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parses client enum strings so an unknown value is a 400, not a framework error. */
public final class Enums {
    private Enums() {}

    /** Null or blank stays null (optional filter). */
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

    /** A null list stays null; a null, blank or unknown element is a 400. Order and duplicates are preserved. */
    public static <E extends Enum<E>> List<E> parseList(Class<E> type, List<String> values, String field) {
        if (values == null) {
            return null;
        }
        List<E> parsed = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " contains a blank value");
            }
            parsed.add(parseOptional(type, value, field));
        }
        return parsed;
    }
}
