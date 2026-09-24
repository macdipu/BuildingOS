package com.buildingos.building.shared.domain.model;

import java.time.Instant;
import java.util.Optional;

/** Undelivered outbox rows and the oldest one's occurrence time, for backlog monitoring. */
public record OutboxBacklog(long pending, Optional<Instant> oldestOccurredAt) {
}
