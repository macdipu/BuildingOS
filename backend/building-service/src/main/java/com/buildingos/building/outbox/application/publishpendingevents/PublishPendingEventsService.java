package com.buildingos.building.outbox.application.publishpendingevents;

import com.buildingos.building.outbox.application.port.out.EventPublisher;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.PendingOutboxEvent;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Publishes due outbox events at least once (UO-10). A failed event stays undelivered with the same event id and is
 * retried after an exponential backoff; later events of the same aggregate wait so consumers see revisions in order.
 */
public class PublishPendingEventsService implements PublishPendingEventsUseCase {
    private final OutboxRepository outbox;
    private final EventPublisher publisher;
    private final UnitOfWork uow;
    private final Clock clock;
    private final int batchSize;
    private final Duration retryBase;
    private final Duration retryMax;

    public PublishPendingEventsService(OutboxRepository outbox, EventPublisher publisher, UnitOfWork uow, Clock clock,
            int batchSize, Duration retryBase, Duration retryMax) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.uow = uow;
        this.clock = clock;
        this.batchSize = batchSize;
        this.retryBase = retryBase;
        this.retryMax = retryMax;
    }

    @Override
    public PublishPendingEventsResult execute() {
        return uow.inTransaction(() -> {
            int published = 0;
            int failed = 0;
            int deferred = 0;
            Set<UUID> blocked = new HashSet<>();
            for (PendingOutboxEvent pending : outbox.lockDue(clock.instant(), batchSize)) {
                UUID aggregate = pending.event().aggregateId();
                if (blocked.contains(aggregate)) {
                    deferred++;
                    continue;
                }
                try {
                    publisher.publish(pending);
                    outbox.markDelivered(pending.event().eventId(), clock.instant());
                    published++;
                } catch (RuntimeException e) {
                    Instant now = clock.instant();
                    outbox.markFailed(pending.event().eventId(), now.plus(backoff(pending.attempts())), describe(e));
                    blocked.add(aggregate);
                    failed++;
                }
            }
            return new PublishPendingEventsResult(published, failed, deferred);
        });
    }

    Duration backoff(int previousAttempts) {
        Duration delay = retryBase;
        for (int i = 0; i < previousAttempts && delay.compareTo(retryMax) < 0; i++) {
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(retryMax) > 0 ? retryMax : delay;
    }

    private static String describe(RuntimeException e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
