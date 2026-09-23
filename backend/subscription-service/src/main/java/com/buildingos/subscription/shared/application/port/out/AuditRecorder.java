package com.buildingos.subscription.shared.application.port.out;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface AuditRecorder {
    record Entry(UUID actorUserId, String action, String entityType, String entityId,
            Map<String, Object> before, Map<String, Object> after, Instant occurredAt) {}

    void record(Entry entry);
}
