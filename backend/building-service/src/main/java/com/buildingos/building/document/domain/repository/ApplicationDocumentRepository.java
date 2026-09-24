package com.buildingos.building.document.domain.repository;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationDocumentRepository {
    void insert(ApplicationDocument document);
    void delete(UUID documentId);
    Optional<ApplicationDocument> find(UUID applicationId, UUID documentId);
    /** Oldest first. */
    List<ApplicationDocument> findByApplication(UUID applicationId);
    int countByApplication(UUID applicationId);
}
