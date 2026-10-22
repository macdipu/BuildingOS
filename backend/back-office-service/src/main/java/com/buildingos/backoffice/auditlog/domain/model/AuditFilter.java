package com.buildingos.backoffice.auditlog.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Filters sent unchanged to every source: since inclusive, until exclusive (with untilId: the keyset
 * {@code (occurredAt, id) < (until, untilId)}), exact entityType/actorUserId.
 */
public record AuditFilter(Instant since, Instant until, UUID untilId, String entityType, UUID actorUserId,
        int limit) {
}
