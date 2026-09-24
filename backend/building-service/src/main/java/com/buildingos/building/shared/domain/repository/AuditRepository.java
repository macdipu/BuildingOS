package com.buildingos.building.shared.domain.repository;

import com.buildingos.building.shared.domain.model.AuditEntry;
import java.util.List;
import java.util.UUID;

public interface AuditRepository {
    void append(AuditEntry entry);
    List<AuditEntry> findByEntity(UUID buildingId, String entityType, UUID entityId);
}
