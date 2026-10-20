package com.buildingos.building.building.application.listplatformbuildings;

import com.buildingos.building.building.domain.model.BuildingStatus;

/** {@code status} null = every status; {@code q} null or blank = no name filter. */
public record ListPlatformBuildingsQuery(BuildingStatus status, String q, int page, int size) {}
