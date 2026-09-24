package com.buildingos.building.building.domain.model;

/** Prerequisites read under the building lock: an active building admin and at least one unit (D-30, UO-09). */
public record ActivationReadiness(long activeAdmins, long units) {}
