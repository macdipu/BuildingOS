package com.buildingos.subscription.audit.application.listauditevents;

import com.buildingos.subscription.audit.domain.model.AuditRecord;
import com.buildingos.subscription.audit.domain.repository.AuditEventRepository;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.NotPermittedException;
import java.util.List;

/** SUPER_ADMIN only (F6-T5c, D-37); {@code audit_event} rows newest first. buildingId and reason are always null. */
public final class ListAuditEventsService implements ListAuditEventsUseCase {
    public static final String SOURCE = "subscription-service";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;
    private final AuditEventRepository audits;

    public ListAuditEventsService(AuditEventRepository audits) {
        this.audits = audits;
    }

    @Override
    public List<AuditEvent> execute(Actor actor, ListAuditEventsQuery query) {
        if (!actor.platformRoles().contains(SUPER_ADMIN)) {
            throw new NotPermittedException();
        }
        int limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be 1-" + MAX_LIMIT);
        }
        return audits.list(query.since(), query.until(), query.entityType(), query.actorUserId(), limit).stream()
                .map(ListAuditEventsService::toEvent)
                .toList();
    }

    private static AuditEvent toEvent(AuditRecord r) {
        return new AuditEvent(r.id(), SOURCE, r.occurredAt(), r.actorUserId(), r.action(), r.entityType(),
                r.entityId(), null, null);
    }
}
