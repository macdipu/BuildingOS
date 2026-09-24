package com.buildingos.building.ownership.domain.model;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.document.domain.model.DocumentType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Metadata of a document attached to an ownership transfer (UO-13); separate from application documents. The bytes
 * live in object storage under an opaque {@link #objectKey()} that never contains the file name.
 */
public record TransferDocument(UUID id, UUID buildingId, UUID transferId, String objectKey, String fileName,
        DocumentType type, long sizeBytes, UUID uploadedBy, Instant uploadedAt) {
    public TransferDocument {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(transferId, "transferId");
        Objects.requireNonNull(objectKey, "objectKey");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(uploadedBy, "uploadedBy");
        Objects.requireNonNull(uploadedAt, "uploadedAt");
        fileName = ApplicationDocument.displayName(fileName);
    }

    public static TransferDocument create(OwnershipTransfer transfer, String fileName, DocumentType type,
            long sizeBytes, UUID uploadedBy, Instant at) {
        UUID id = UUID.randomUUID();
        return new TransferDocument(id, transfer.buildingId(), transfer.id(),
                "ownership-transfers/" + transfer.id() + "/" + id, fileName, type, sizeBytes, uploadedBy, at);
    }

    /** The transfer's source or recipient. */
    public static boolean isParty(OwnershipTransfer transfer, UUID userId) {
        return transfer.sourceOwnerUserId().equals(userId) || transfer.recipientUserId().equals(userId);
    }
}
