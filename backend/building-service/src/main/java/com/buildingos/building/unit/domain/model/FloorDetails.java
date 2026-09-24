package com.buildingos.building.unit.domain.model;

/** Validated floor metadata; the label is display data, {@code normalizedLabel} keys uniqueness. */
public record FloorDetails(String label, FloorKind kind, int displayOrder) {
    public static final int MAX_LABEL = 40;

    public FloorDetails {
        label = UnitNames.display(label, MAX_LABEL, "label", "FLOOR_LABEL");
        if (kind == null) {
            throw new InvalidUnitInputException("FLOOR_KIND_REQUIRED", "kind", "kind is required");
        }
    }

    public static FloorDetails of(String label, FloorKind kind, Integer displayOrder) {
        if (displayOrder == null) {
            throw new InvalidUnitInputException("FLOOR_ORDER_REQUIRED", "displayOrder", "displayOrder is required");
        }
        return new FloorDetails(label, kind, displayOrder);
    }

    public String normalizedLabel() { return UnitNames.normalized(label); }
}
