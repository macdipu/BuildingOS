package com.buildingos.building.shared.domain.repository;

import com.buildingos.building.shared.domain.model.OutboxEvent;

public interface OutboxRepository {
    /** Must run in the transaction of the write the event describes. */
    void append(OutboxEvent event);
}
