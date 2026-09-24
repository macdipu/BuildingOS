package com.buildingos.building.buildingapplication.domain.model;

/** Human-readable application reference {@code BA-yyyy-nnnnnn}. */
public record ApplicationNumber(String value) {
    public ApplicationNumber {
        if (value == null || !value.matches("BA-\\d{4}-\\d{6,9}")) {
            throw new IllegalArgumentException("Invalid application number");
        }
    }

    public static ApplicationNumber of(int year, long sequence) {
        return new ApplicationNumber("BA-%04d-%06d".formatted(year, sequence));
    }
}
