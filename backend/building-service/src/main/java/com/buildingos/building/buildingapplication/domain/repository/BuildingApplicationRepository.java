package com.buildingos.building.buildingapplication.domain.repository;

import com.buildingos.building.buildingapplication.domain.model.ApplicationStatus;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingApplicationRepository {
    long nextNumberSequence();
    void insert(BuildingApplication application);
    /** Callers hold the row lock from {@link #findByIdForUpdate}. */
    void update(BuildingApplication application);
    Optional<BuildingApplication> findById(UUID id);
    /** Row-locks the application for the surrounding unit of work. */
    Optional<BuildingApplication> findByIdForUpdate(UUID id);
    /** Newest first. */
    List<BuildingApplication> findByApplicant(UUID applicantUserId);
    /** Submitted applications only (drafts stay private to the applicant); {@code status} null = all; oldest submitted first. */
    List<BuildingApplication> findPage(ApplicationStatus status, int offset, int limit);
    long count(ApplicationStatus status);
}
