package com.buildingos.building.unit.application.batch;

import java.util.UUID;

/** One raw batch row as supplied (JSON, generator or sheet); the floor is given by id or by label. */
public record BatchRowInput(int rowNumber, String number, UUID floorId, String floorLabel, String type,
        String areaSqft, String bedrooms, String defaultMaintenanceRate, String notes) {}
