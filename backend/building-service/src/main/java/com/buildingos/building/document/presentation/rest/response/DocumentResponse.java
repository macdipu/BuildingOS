package com.buildingos.building.document.presentation.rest.response;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(UUID id, String fileName, String contentType, long sizeBytes, Instant uploadedAt) {
    public static DocumentResponse of(ApplicationDocument d) {
        return new DocumentResponse(d.id(), d.fileName(), d.type().contentType(), d.sizeBytes(), d.uploadedAt());
    }
}
