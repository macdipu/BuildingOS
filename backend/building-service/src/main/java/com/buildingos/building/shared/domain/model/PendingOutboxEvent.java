package com.buildingos.building.shared.domain.model;

import java.util.Objects;

/** A committed, undelivered outbox row; {@code correlationId} is the originating request's trace id, if any. */
public record PendingOutboxEvent(OutboxEvent event, String correlationId, int attempts) {
    public PendingOutboxEvent {
        Objects.requireNonNull(event, "event");
    }
}
