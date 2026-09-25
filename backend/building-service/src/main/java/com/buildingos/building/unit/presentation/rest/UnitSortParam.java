package com.buildingos.building.unit.presentation.rest;

import com.buildingos.building.unit.domain.model.UnitSort;
import java.util.Locale;

/** Parses {@code sort=unitNumber|floor|type[,asc|desc]} and {@code q} for the unit list (BRD §48). */
final class UnitSortParam {
    private static final int MAX_QUERY_LENGTH = 32;

    private UnitSortParam() {}

    static UnitSort parse(String value) {
        if (value == null || value.isBlank()) {
            return UnitSort.DEFAULT;
        }
        String[] parts = value.split(",", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("sort has an unknown value: " + value);
        }
        UnitSort.Field field = switch (parts[0].trim()) {
            case "unitNumber" -> UnitSort.Field.UNIT_NUMBER;
            case "floor" -> UnitSort.Field.FLOOR;
            case "type" -> UnitSort.Field.TYPE;
            default -> throw new IllegalArgumentException("sort has an unknown value: " + value);
        };
        String direction = parts.length == 2 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
        return switch (direction) {
            case "asc" -> new UnitSort(field, false);
            case "desc" -> new UnitSort(field, true);
            default -> throw new IllegalArgumentException("sort has an unknown direction: " + value);
        };
    }

    static String numberQuery(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String trimmed = q.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("q must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        return trimmed;
    }
}
