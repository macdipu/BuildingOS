package com.buildingos.building.document.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadata of a verification document; the bytes live in object storage under {@link #objectKey()}.
 * The original file name is display metadata only and never part of the key.
 */
public record ApplicationDocument(UUID id, UUID applicationId, String objectKey, String fileName, DocumentType type,
        long sizeBytes, UUID uploadedBy, Instant uploadedAt) {
    private static final int MAX_FILE_NAME = 255;

    public ApplicationDocument {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(objectKey, "objectKey");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(uploadedBy, "uploadedBy");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        fileName = displayName(fileName);
    }

    public static ApplicationDocument create(UUID applicationId, String fileName, DocumentType type, long sizeBytes,
            UUID uploadedBy, Instant at) {
        UUID id = UUID.randomUUID();
        return new ApplicationDocument(id, applicationId, "applications/" + applicationId + "/" + id, fileName, type,
                sizeBytes, uploadedBy, at);
    }

    /** Last path segment, control characters and quotes removed, bounded; "document" when nothing is left. */
    public static String displayName(String raw) {
        if (raw == null) {
            return "document";
        }
        String name = raw.substring(Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\')) + 1)
                .replaceAll("[\\p{Cntrl}\"]", "").strip();
        if (name.length() > MAX_FILE_NAME) {
            name = name.substring(name.length() - MAX_FILE_NAME);
        }
        return name.isEmpty() ? "document" : name;
    }
}
