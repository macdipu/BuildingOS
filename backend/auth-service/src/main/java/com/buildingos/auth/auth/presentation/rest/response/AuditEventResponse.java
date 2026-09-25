package com.buildingos.auth.auth.presentation.rest.response;

import com.buildingos.auth.auth.application.listauditevents.AuditEvent;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(UUID id, String source, Instant occurredAt, UUID actorUserId, String action,
        String entityType, UUID entityId, UUID buildingId, String reason, String role) {
    public static AuditEventResponse of(AuditEvent event) {
        return new AuditEventResponse(event.id(), event.source(), event.occurredAt(), event.actorUserId(),
                event.action(), event.entityType(), event.entityId(), event.buildingId(), event.reason(),
                event.role());
    }
}
