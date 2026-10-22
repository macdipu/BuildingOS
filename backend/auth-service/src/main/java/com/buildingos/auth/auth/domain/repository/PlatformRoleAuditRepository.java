package com.buildingos.auth.auth.domain.repository;

import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlatformRoleAuditRepository {
    void append(PlatformRoleAuditEntry entry);

    /**
     * Entries with {@code since <= occurredAt < until} (each bound ignored if null) by {@code actorUserId}
     * (ignored if null), newest first ({@code occurredAt}, then id), at most {@code limit}. A non-null
     * {@code untilId} widens the upper bound to the keyset {@code (occurredAt, id) < (until, untilId)}.
     */
    List<PlatformRoleAuditEntry> list(Instant since, Instant until, UUID untilId, UUID actorUserId, int limit);
}
