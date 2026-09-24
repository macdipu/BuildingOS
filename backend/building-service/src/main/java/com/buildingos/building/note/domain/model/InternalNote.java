package com.buildingos.building.note.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Back-office note on an application (BRD 149.5); never shown to the applicant. */
public record InternalNote(UUID id, UUID applicationId, UUID authorUserId, String body, Instant createdAt) {
    private static final int MAX_BODY_LENGTH = 2000;

    public InternalNote {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(authorUserId, "authorUserId");
        Objects.requireNonNull(createdAt, "createdAt");
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Note body is required");
        }
        body = body.strip();
        if (body.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Note body must be at most " + MAX_BODY_LENGTH + " characters");
        }
    }
}
