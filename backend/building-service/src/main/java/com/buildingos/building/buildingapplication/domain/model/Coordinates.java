package com.buildingos.building.buildingapplication.domain.model;

/** WGS84 point supplied by the applicant or device; a review signal only (BRD 149.6). */
public record Coordinates(double latitude, double longitude) {
    public Coordinates {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordinates are out of range");
        }
    }

    /** Both or neither. */
    public static Coordinates ofNullable(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return null;
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude must be given together");
        }
        return new Coordinates(latitude, longitude);
    }
}
