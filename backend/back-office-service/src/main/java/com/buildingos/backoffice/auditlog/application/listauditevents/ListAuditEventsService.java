package com.buildingos.backoffice.auditlog.application.listauditevents;

import com.buildingos.backoffice.auditlog.application.port.out.RemoteAuditLog;
import com.buildingos.backoffice.auditlog.domain.model.AuditEvent;
import com.buildingos.backoffice.auditlog.domain.model.AuditFilter;
import com.buildingos.backoffice.auditlog.domain.model.AuditPage;
import com.buildingos.backoffice.auditlog.domain.model.AuditSource;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

/**
 * SUPER_ADMIN only (F6-T5d, BOC-05, D-37). Queries every selected source in parallel with the same filters and
 * limit, merges newest first and truncates to {@code limit}. A source that fails or exceeds {@code timeout} is
 * reported in {@code unavailableSources}; the others are still returned.
 */
public final class ListAuditEventsService implements ListAuditEventsUseCase {
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;
    /** Own transitions carry no action column: {@code FROM->TO} (arrow U+2192), FROM = NONE on creation. */
    public static final String NO_STATUS = "NONE";
    public static final String ARROW = "→";

    private final RemoteAuditLog remote;
    private final LifecycleTransitionRepository transitions;
    private final Duration timeout;
    private final UnaryOperator<Runnable> callerContext;

    /**
     * @param callerContext wraps each task, on the request thread, so it runs with the caller's security context
     *     (needed for the bearer-token relay on pool threads)
     */
    public ListAuditEventsService(RemoteAuditLog remote, LifecycleTransitionRepository transitions, Duration timeout,
            UnaryOperator<Runnable> callerContext) {
        this.remote = remote;
        this.transitions = transitions;
        this.timeout = timeout;
        this.callerContext = callerContext;
    }

    @Override
    public AuditPage execute(Actor actor, ListAuditEventsQuery query) {
        if (!actor.isSuperAdmin()) {
            throw new NotPermittedException();
        }
        int limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be 1-" + MAX_LIMIT);
        }
        var filter = new AuditFilter(query.since(), query.until(), query.entityType(), query.actorUserId(), limit);
        List<AuditSource> sources = query.source() == null ? List.of(AuditSource.values()) : List.of(query.source());

        ExecutorService pool = Executors.newFixedThreadPool(sources.size());
        Executor withCaller = task -> pool.execute(callerContext.apply(task));
        try {
            List<CompletableFuture<Optional<List<AuditEvent>>>> calls = sources.stream()
                    .map(source -> CompletableFuture.supplyAsync(() -> Optional.of(fetch(source, filter)), withCaller)
                            .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                            .exceptionally(failed -> Optional.empty()))
                    .toList();
            List<List<AuditEvent>> found = new ArrayList<>();
            List<AuditSource> unavailable = new ArrayList<>();
            for (int i = 0; i < sources.size(); i++) {
                Optional<List<AuditEvent>> result = calls.get(i).join();
                if (result.isPresent()) {
                    found.add(result.get());
                } else {
                    unavailable.add(sources.get(i));
                }
            }
            return AuditPage.merge(found, limit, unavailable);
        } finally {
            pool.shutdownNow();
        }
    }

    private List<AuditEvent> fetch(AuditSource source, AuditFilter filter) {
        return source == AuditSource.BACK_OFFICE_SERVICE ? own(filter) : remote.fetch(source, filter);
    }

    /** An entityType this service does not own matches nothing here. */
    private List<AuditEvent> own(AuditFilter filter) {
        EntityType type = null;
        if (filter.entityType() != null) {
            type = Arrays.stream(EntityType.values()).filter(t -> t.name().equals(filter.entityType())).findFirst()
                    .orElse(null);
            if (type == null) {
                return List.of();
            }
        }
        return transitions.list(filter.since(), filter.until(), type, filter.actorUserId(), filter.limit()).stream()
                .map(ListAuditEventsService::toEvent).toList();
    }

    private static AuditEvent toEvent(LifecycleTransition t) {
        String from = t.fromStatus() == null ? NO_STATUS : t.fromStatus();
        return new AuditEvent(t.id(), AuditSource.BACK_OFFICE_SERVICE, t.occurredAt(), t.actorUserId(),
                from + ARROW + t.toStatus(), t.entityType().name(), t.entityId().toString(), null, t.reason(), null);
    }
}
