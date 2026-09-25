package com.buildingos.subscription.audit.domain.repository;

import com.buildingos.subscription.audit.domain.model.AuditRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read side of {@code audit_event}; rows are written by {@code AuditRecorder}. */
public interface AuditEventRepository {
    /**
     * Rows with {@code since <= occurredAt < until}, of {@code entityType}, by {@code actorUserId} (each filter ignored
     * if null), newest first ({@code occurredAt}, then id), at most {@code limit}.
     */
    List<AuditRecord> list(Instant since, Instant until, String entityType, UUID actorUserId, int limit);
}
