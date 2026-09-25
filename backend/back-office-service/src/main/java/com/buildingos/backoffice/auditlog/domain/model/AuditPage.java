package com.buildingos.backoffice.auditlog.domain.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Merged audit view: newest first ({@code occurredAt}, then id, both descending), at most {@code limit} items.
 * {@code nextUntil} is the oldest returned {@code occurredAt} when the page is full (pass it as {@code until} for the
 * next page), else null. {@code unavailableSources} lists sources that failed or timed out.
 */
public record AuditPage(List<AuditEvent> items, Instant nextUntil, List<AuditSource> unavailableSources) {
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
        Instant nextUntil = items.size() < limit ? null : items.get(items.size() - 1).occurredAt();
        return new AuditPage(items, nextUntil, unavailableSources);
    }
}
