package com.buildingos.building.audit.application.listauditevents;

import java.time.Instant;
import java.util.UUID;

/** {@code since} inclusive, {@code until} exclusive; null filters are ignored; a null {@code limit} means the default. */
public record ListAuditEventsQuery(Instant since, Instant until, String entityType, UUID actorUserId, Integer limit) {
}
