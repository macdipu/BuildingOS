package com.buildingos.building.shared.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * An event committed with the domain write that caused it (UO-10); a publisher delivers it later with the same
 * {@code eventId} so consumers can deduplicate. {@code data} holds contract fields only, never contact details.
 */
public record OutboxEvent(UUID eventId, String eventType, int eventVersion, UUID buildingId, String aggregateType,
        UUID aggregateId, long aggregateRevision, Map<String, Object> data, Instant occurredAt) {
    public OutboxEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(aggregateType, "aggregateType");
        Objects.requireNonNull(aggregateId, "aggregateId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        data = Map.copyOf(data);
    }
}
