package com.buildingos.building.shared.domain.repository;

import com.buildingos.building.shared.domain.model.OutboxBacklog;
import com.buildingos.building.shared.domain.model.OutboxEvent;
import com.buildingos.building.shared.domain.model.PendingOutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {
    /** Must run in the transaction of the write the event describes. */
    void append(OutboxEvent event);

    /**
     * Locks up to {@code limit} undelivered events due at {@code now}, oldest first, skipping rows another publisher
     * holds. The locks last until the surrounding transaction ends.
     */
    List<PendingOutboxEvent> lockDue(Instant now, int limit);

    void markDelivered(UUID eventId, Instant deliveredAt);

    void markFailed(UUID eventId, Instant nextAttemptAt, String error);

    OutboxBacklog backlog();
}
