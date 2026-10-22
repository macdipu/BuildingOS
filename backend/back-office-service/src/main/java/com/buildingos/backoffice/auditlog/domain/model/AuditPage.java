package com.buildingos.backoffice.auditlog.domain.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Merged audit view: newest first ({@code occurredAt}, then id, both descending), at most {@code limit} items.
 * When the page is full, {@code (nextUntil, nextUntilId)} is the last item's {@code (occurredAt, id)}: pass them as
 * {@code until}/{@code untilId} for the next page; else both null. Every source orders by the same key, so the keyset
 * neither drops nor repeats rows sharing an {@code occurredAt} (ids are random UUIDs, unique across sources).
 * {@code unavailableSources} lists sources that failed or timed out.
 */
public record AuditPage(List<AuditEvent> items, Instant nextUntil, UUID nextUntilId,
        List<AuditSource> unavailableSources) {
    /** Same order as the sources' SQL: uuid text order matches PostgreSQL's uuid order. */
    private static final Comparator<AuditEvent> NEWEST_FIRST = Comparator.comparing(AuditEvent::occurredAt)
            .thenComparing(event -> event.id().toString())
            .reversed();

    public AuditPage {
        items = List.copyOf(items);
        unavailableSources = List.copyOf(unavailableSources);
    }

    public static AuditPage merge(Collection<List<AuditEvent>> perSource, int limit,
            List<AuditSource> unavailableSources) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be positive");
        }
        List<AuditEvent> items = perSource.stream().flatMap(List::stream).sorted(NEWEST_FIRST).limit(limit).toList();
        AuditEvent last = items.size() < limit ? null : items.get(items.size() - 1);
        return new AuditPage(items, last == null ? null : last.occurredAt(), last == null ? null : last.id(),
                unavailableSources);
    }
}
