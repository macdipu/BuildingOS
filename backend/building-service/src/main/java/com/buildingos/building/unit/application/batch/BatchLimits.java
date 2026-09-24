package com.buildingos.building.unit.application.batch;

/** Technical safeguards from TECH-SPEC-F4 "Bulk validation"; not subscription limits. */
public record BatchLimits(int maxRows, long maxSheetBytes) {
    public BatchLimits {
        if (maxRows < 1 || maxSheetBytes < 1) {
            throw new IllegalArgumentException("Batch limits must be positive");
        }
    }
}
