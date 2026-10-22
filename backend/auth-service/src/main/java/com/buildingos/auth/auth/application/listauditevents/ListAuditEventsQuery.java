package com.buildingos.auth.auth.application.listauditevents;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * {@code callerPlatformRoles} come from the caller's validated access token. {@code since} inclusive,
 * {@code until} exclusive; with {@code untilId} the bound is the keyset {@code (occurredAt, id) < (until, untilId)}
 * (requires {@code until}). Null filters are ignored; a null {@code limit} means the default.
 */
public record ListAuditEventsQuery(Set<String> callerPlatformRoles, Instant since, Instant until, UUID untilId,
        String entityType, UUID actorUserId, Integer limit) {
    public ListAuditEventsQuery {
        callerPlatformRoles = Set.copyOf(callerPlatformRoles);
    }
}
