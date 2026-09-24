package com.buildingos.building.buildingapplication.application.listapplications;

import com.buildingos.building.buildingapplication.domain.model.ApplicationStatus;

/** {@code status} null = every submitted application. */
public record ListApplicationsQuery(ApplicationStatus status, int page, int size) {}
