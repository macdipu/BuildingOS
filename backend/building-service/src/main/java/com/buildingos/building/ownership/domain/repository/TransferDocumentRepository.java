package com.buildingos.building.ownership.domain.repository;

import com.buildingos.building.ownership.domain.model.TransferDocument;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Removed documents are kept as audit evidence and are invisible to every read here. */
public interface TransferDocumentRepository {
    void insert(TransferDocument document);
    int countActive(UUID buildingId, UUID transferId);
    List<TransferDocument> listActive(UUID buildingId, UUID transferId);
    Optional<TransferDocument> findActive(UUID buildingId, UUID transferId, UUID documentId);
    void markRemoved(UUID documentId, UUID removedBy, Instant removedAt, String reason);
}
