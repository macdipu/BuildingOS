package com.buildingos.building.unit.presentation.rest.request;

/** {@code expectedVersion} is required on update only; {@code reason} is required for platform admins. */
public record FloorRequest(String label, String kind, Integer displayOrder, Long expectedVersion, String reason) {}
