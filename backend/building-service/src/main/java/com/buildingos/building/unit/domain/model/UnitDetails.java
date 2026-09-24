package com.buildingos.building.unit.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BRD §49 unit fields, one validator for individual and batch input (UO-02/04). Area is square feet and the
 * maintenance rate is metadata only, both with at most 2 decimals; extra precision is rejected, never rounded.
 */
public record UnitDetails(String number, UUID floorId, UnitType type, BigDecimal areaSqft, Integer bedrooms,
        BigDecimal defaultMaintenanceRate, String notes) {
    public static final int MAX_NUMBER = 32;
    public static final int MAX_NOTES = 1000;
    private static final int SCALE = 2;
    private static final int INTEGER_DIGITS = 10;

    public UnitDetails {
        number = UnitNames.display(number, MAX_NUMBER, "number", "UNIT_NUMBER");
        if (floorId == null) {
            throw new InvalidUnitInputException("FLOOR_REQUIRED", "floorId", "floorId is required");
        }
        if (type == null) {
            throw new InvalidUnitInputException("UNIT_TYPE_REQUIRED", "type", "type is required");
        }
        if (areaSqft == null) {
            throw new InvalidUnitInputException("AREA_REQUIRED", "areaSqft", "areaSqft is required");
        }
        if (areaSqft.signum() <= 0) {
            throw new InvalidUnitInputException("AREA_NOT_POSITIVE", "areaSqft", "areaSqft must be positive");
        }
        areaSqft = money(areaSqft, "areaSqft", "AREA_PRECISION");
        if (bedrooms != null && (bedrooms < 0 || bedrooms > Short.MAX_VALUE)) {
            throw new InvalidUnitInputException("BEDROOMS_INVALID", "bedrooms", "bedrooms must be zero or more");
        }
        if (defaultMaintenanceRate != null) {
            if (defaultMaintenanceRate.signum() < 0) {
                throw new InvalidUnitInputException("MAINTENANCE_RATE_NEGATIVE", "defaultMaintenanceRate",
                        "defaultMaintenanceRate must not be negative");
            }
            defaultMaintenanceRate = money(defaultMaintenanceRate, "defaultMaintenanceRate",
                    "MAINTENANCE_RATE_PRECISION");
        }
        if (notes != null) {
            notes = notes.isBlank() ? null : notes.strip();
            if (notes != null && notes.length() > MAX_NOTES) {
                throw new InvalidUnitInputException("NOTES_TOO_LONG", "notes",
                        "notes must be at most " + MAX_NOTES + " characters");
            }
        }
    }

    public String normalizedNumber() { return UnitNames.normalized(number); }

    private static BigDecimal money(BigDecimal value, String field, String code) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() > SCALE || stripped.precision() - stripped.scale() > INTEGER_DIGITS) {
            throw new InvalidUnitInputException(code, field,
                    field + " allows " + INTEGER_DIGITS + " digits and " + SCALE + " decimals");
        }
        return value.setScale(SCALE);
    }
}
