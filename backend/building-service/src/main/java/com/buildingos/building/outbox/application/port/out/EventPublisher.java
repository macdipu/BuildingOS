package com.buildingos.building.outbox.application.port.out;

import com.buildingos.building.shared.domain.model.PendingOutboxEvent;

/** Delivers one committed event to the broker and returns only once it is acknowledged; failures throw. */
public interface EventPublisher {
    void publish(PendingOutboxEvent event);
}
