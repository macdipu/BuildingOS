package com.buildingos.auth.auth.domain.repository;

import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlatformRoleAuditRepository {
    void append(PlatformRoleAuditEntry entry);

    /**
     * Entries with {@code since <= occurredAt < until} (each bound ignored if null) by {@code actorUserId}
     * (ignored if null), newest first, at most {@code limit}.
     */
    List<PlatformRoleAuditEntry> list(Instant since, Instant until, UUID actorUserId, int limit);
}
