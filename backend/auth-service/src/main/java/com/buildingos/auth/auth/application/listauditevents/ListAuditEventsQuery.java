package com.buildingos.auth.auth.application.listauditevents;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * {@code callerPlatformRoles} come from the caller's validated access token. {@code since} inclusive,
 * {@code until} exclusive; null filters are ignored; a null {@code limit} means the default.
 */
public record ListAuditEventsQuery(Set<String> callerPlatformRoles, Instant since, Instant until,
        String entityType, UUID actorUserId, Integer limit) {
    public ListAuditEventsQuery {
        callerPlatformRoles = Set.copyOf(callerPlatformRoles);
    }
}
