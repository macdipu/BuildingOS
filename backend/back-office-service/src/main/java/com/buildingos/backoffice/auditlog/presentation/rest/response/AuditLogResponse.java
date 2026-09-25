package com.buildingos.backoffice.auditlog.presentation.rest.response;

import com.buildingos.backoffice.auditlog.domain.model.AuditEvent;
import com.buildingos.backoffice.auditlog.domain.model.AuditPage;
import com.buildingos.backoffice.auditlog.domain.model.AuditSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code nextUntil} is null when fewer than {@code limit} items were returned. */
public record AuditLogResponse(List<Item> items, Instant nextUntil, List<String> unavailableSources) {
    /** InternalAuditEvent shape; {@code role} is omitted unless the row carries one (auth-service). */
    public record Item(UUID id, String source, Instant occurredAt, UUID actorUserId, String action,
            String entityType, String entityId, UUID buildingId, String reason,
            @JsonInclude(JsonInclude.Include.NON_NULL) String role) {
        static Item of(AuditEvent event) {
            return new Item(event.id(), event.source().wireName(), event.occurredAt(), event.actorUserId(),
                    event.action(), event.entityType(), event.entityId(), event.buildingId(), event.reason(),
                    event.role());
        }
    }

    public static AuditLogResponse of(AuditPage page) {
        return new AuditLogResponse(page.items().stream().map(Item::of).toList(), page.nextUntil(),
                page.unavailableSources().stream().map(AuditSource::wireName).toList());
    }
}
