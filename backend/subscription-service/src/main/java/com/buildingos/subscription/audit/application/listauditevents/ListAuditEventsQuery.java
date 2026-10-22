package com.buildingos.subscription.audit.application.listauditevents;

import java.time.Instant;
import java.util.UUID;

/**
 * {@code since} inclusive, {@code until} exclusive; with {@code untilId} the bound is the keyset
 * {@code (occurredAt, id) < (until, untilId)} (requires {@code until}). Null filters are ignored; a null {@code limit}
 * means the default.
 */
public record ListAuditEventsQuery(Instant since, Instant until, UUID untilId, String entityType, UUID actorUserId,
        Integer limit) {
}
