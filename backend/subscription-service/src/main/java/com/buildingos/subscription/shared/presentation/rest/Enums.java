package com.buildingos.subscription.shared.presentation.rest;

import java.util.Locale;

/** Parses client enum strings so an unknown value is a 400, not a framework error. */
public final class Enums {
    private Enums() {}

    public static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException(field + " has an unknown value: " + value);
        }
    }
}
