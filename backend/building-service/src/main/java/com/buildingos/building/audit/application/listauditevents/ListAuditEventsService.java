package com.buildingos.building.audit.application.listauditevents;

import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.NotPermittedException;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * SUPER_ADMIN only (F6-T5c, D-37). Merges {@code lifecycle_transition} and {@code building_audit} rows newest first
 * ({@code occurredAt}, then id) and truncates to {@code limit}; each source is queried with the same limit.
 */
public final class ListAuditEventsService implements ListAuditEventsUseCase {
    public static final String SOURCE = "building-service";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;
    /** Transition rows carry no action column: the action is {@code FROM->TO} (arrow U+2192), FROM = NONE on creation. */
    public static final String NO_STATUS = "NONE";
    public static final String ARROW = "→";
    /** Same order as the SQL: occurred_at DESC, id DESC (uuid text order matches PostgreSQL's uuid order). */
    private static final Comparator<AuditEvent> NEWEST_FIRST = Comparator.comparing(AuditEvent::occurredAt)
            .thenComparing(event -> event.id().toString())
            .reversed();

    private final LifecycleTransitionRepository transitions;
    private final AuditRepository audits;

    public ListAuditEventsService(LifecycleTransitionRepository transitions, AuditRepository audits) {
        this.transitions = transitions;
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
        var fromTransitions = transitions.list(query.since(), query.until(), query.entityType(), query.actorUserId(),
                limit).stream().map(ListAuditEventsService::toEvent);
        var fromAudit = audits.list(query.since(), query.until(), query.entityType(), query.actorUserId(), limit)
                .stream().map(ListAuditEventsService::toEvent);
        return Stream.concat(fromTransitions, fromAudit).sorted(NEWEST_FIRST).limit(limit).toList();
    }

    private static AuditEvent toEvent(LifecycleTransition t) {
        String from = t.fromStatus() == null ? NO_STATUS : t.fromStatus();
        var buildingId = t.entityType() == EntityType.BUILDING ? t.entityId() : null;
        return new AuditEvent(t.id(), SOURCE, t.occurredAt(), t.actorUserId(), from + ARROW + t.toStatus(),
                t.entityType().name(), t.entityId(), buildingId, t.reason());
    }

    private static AuditEvent toEvent(AuditEntry e) {
        return new AuditEvent(e.id(), SOURCE, e.occurredAt(), e.actorUserId(), e.action(), e.entityType(),
                e.entityId(), e.buildingId(), e.reason());
    }
}
