package com.buildingos.building.ownership.presentation.rest.response;

import com.buildingos.building.ownership.domain.model.TransferDocument;
import java.time.Instant;
import java.util.UUID;

public record TransferDocumentResponse(UUID id, UUID transferId, String fileName, String contentType, long sizeBytes,
        Instant uploadedAt) {
    public static TransferDocumentResponse of(TransferDocument d) {
        return new TransferDocumentResponse(d.id(), d.transferId(), d.fileName(), d.type().contentType(),
                d.sizeBytes(), d.uploadedAt());
    }
}
