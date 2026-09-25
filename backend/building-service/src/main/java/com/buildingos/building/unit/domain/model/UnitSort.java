package com.buildingos.building.unit.domain.model;

/** Unit list ordering; unit number then id always break ties so pages are stable. */
public record UnitSort(Field field, boolean descending) {
    public enum Field { UNIT_NUMBER, FLOOR, TYPE }

    public static final UnitSort DEFAULT = new UnitSort(Field.UNIT_NUMBER, false);

    public UnitSort {
        if (field == null) {
            throw new IllegalArgumentException("sort field is required");
        }
    }
}
