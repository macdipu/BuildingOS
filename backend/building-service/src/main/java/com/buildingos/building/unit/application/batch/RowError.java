package com.buildingos.building.unit.application.batch;

/** Localizable per-row error: the client maps {@code code} to en/bn text. */
public record RowError(String field, String code, String message) {}
