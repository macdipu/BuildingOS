package com.buildingos.building.buildingapplication.domain.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A request to bring a building onto the platform (BRD 149.3-149.4). Never a building itself; approval creates one.
 * Every transition returns a new instance with {@code version} incremented.
 */
public record BuildingApplication(UUID id, ApplicationNumber number, UUID applicantUserId, ApplicationDetails details,
        ApplicationSource source, ApplicationStatus status, Instant submittedAt, Instant reviewedAt, UUID reviewedBy,
        String rejectionReason, String infoRequestMessage, int version, Instant createdAt, Instant updatedAt) {
    private static final Set<ApplicationStatus> EDITABLE =
            EnumSet.of(ApplicationStatus.DRAFT, ApplicationStatus.MORE_INFORMATION_REQUIRED);
    private static final int MAX_REASON_LENGTH = 1000;

    public BuildingApplication {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(number, "number");
        Objects.requireNonNull(applicantUserId, "applicantUserId");
        Objects.requireNonNull(details, "details");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(status, "status");
    }

    public static BuildingApplication draft(UUID id, ApplicationNumber number, UUID applicant, ApplicationDetails details,
            Instant at) {
        return new BuildingApplication(id, number, applicant, details, ApplicationSource.SELF_SERVICE,
                ApplicationStatus.DRAFT, null, null, null, null, null, 0, at, at);
    }

    public boolean isEditable() { return EDITABLE.contains(status); }

    public boolean isOwnedBy(UUID userId) { return applicantUserId.equals(userId); }

    public BuildingApplication edited(ApplicationDetails newDetails, Instant at) {
        if (!isEditable()) {
            throw new ApplicationNotEditableException(status);
        }
        return with(newDetails, status, submittedAt, reviewedAt, reviewedBy, rejectionReason, infoRequestMessage, at);
    }

    public BuildingApplication submitted(Instant at) {
        if (!isEditable()) {
            throw new InvalidTransitionException(status, "submit");
        }
        var missing = details.missingForSubmission();
        if (!missing.isEmpty()) {
            throw new IncompleteApplicationException(missing);
        }
        return with(details, ApplicationStatus.SUBMITTED, at, reviewedAt, reviewedBy, rejectionReason,
                infoRequestMessage, at);
    }

    public BuildingApplication reviewStarted(UUID reviewer, Instant at) {
        require(ApplicationStatus.SUBMITTED, "start review");
        return with(details, ApplicationStatus.UNDER_REVIEW, submittedAt, reviewedAt, reviewer, rejectionReason,
                infoRequestMessage, at);
    }

    public BuildingApplication informationRequested(UUID reviewer, String message, Instant at) {
        require(ApplicationStatus.UNDER_REVIEW, "request information");
        return with(details, ApplicationStatus.MORE_INFORMATION_REQUIRED, submittedAt, at, reviewer, rejectionReason,
                reason(message, "message"), at);
    }

    public BuildingApplication rejected(UUID reviewer, String reason, Instant at) {
        require(ApplicationStatus.UNDER_REVIEW, "reject");
        return with(details, ApplicationStatus.REJECTED, submittedAt, at, reviewer, reason(reason, "reason"),
                infoRequestMessage, at);
    }

    public BuildingApplication approved(UUID reviewer, Instant at) {
        require(ApplicationStatus.UNDER_REVIEW, "approve");
        return with(details, ApplicationStatus.APPROVED, submittedAt, at, reviewer, rejectionReason,
                infoRequestMessage, at);
    }

    /** Audit reason for sensitive actions (BRD 149.5): required, trimmed, bounded. */
    public static String reason(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String trimmed = value.strip();
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException(field + " must be at most " + MAX_REASON_LENGTH + " characters");
        }
        return trimmed;
    }

    private void require(ApplicationStatus expected, String action) {
        if (status != expected) {
            throw new InvalidTransitionException(status, action);
        }
    }

    private BuildingApplication with(ApplicationDetails newDetails, ApplicationStatus newStatus, Instant newSubmittedAt,
            Instant newReviewedAt, UUID newReviewedBy, String newRejectionReason, String newInfoRequestMessage,
            Instant at) {
        return new BuildingApplication(id, number, applicantUserId, newDetails, source, newStatus, newSubmittedAt,
                newReviewedAt, newReviewedBy, newRejectionReason, newInfoRequestMessage, version + 1, createdAt, at);
    }
}
