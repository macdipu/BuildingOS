package com.buildingos.building.shared.domain.repository;

import com.buildingos.building.shared.domain.model.AuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditRepository {
    void append(AuditEntry entry);
    List<AuditEntry> findByEntity(UUID buildingId, String entityType, UUID entityId);

    /**
     * Entries with {@code since <= occurredAt < until}, of {@code entityType}, by {@code actorUserId} (each filter
     * ignored if null), newest first ({@code occurredAt}, then id), at most {@code limit} (F6-T5c). A non-null
     * {@code untilId} widens the upper bound to the keyset {@code (occurredAt, id) < (until, untilId)}.
     */
    List<AuditEntry> list(Instant since, Instant until, UUID untilId, String entityType, UUID actorUserId,
            int limit);
}
