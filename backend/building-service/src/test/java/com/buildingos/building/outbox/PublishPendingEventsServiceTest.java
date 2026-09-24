package com.buildingos.building.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.outbox.application.port.out.EventPublisher;
import com.buildingos.building.outbox.application.publishpendingevents.PublishPendingEventsResult;
import com.buildingos.building.outbox.application.publishpendingevents.PublishPendingEventsService;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.OutboxBacklog;
import com.buildingos.building.shared.domain.model.OutboxEvent;
import com.buildingos.building.shared.domain.model.PendingOutboxEvent;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** F4-T5a: retry backoff, same-aggregate ordering and at-least-once bookkeeping, without a broker. */
class PublishPendingEventsServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final UnitOfWork DIRECT = new UnitOfWork() {
        @Override
        public <T> T inTransaction(Supplier<T> work) { return work.get(); }
    };

    private final FakeOutbox outbox = new FakeOutbox();
    private final List<UUID> sent = new ArrayList<>();
    private final Set<UUID> broken = new HashSet<>();
    private final EventPublisher publisher = pending -> {
        if (broken.contains(pending.event().eventId())) {
            throw new IllegalStateException("publishing failed", new RuntimeException("broker unavailable"));
        }
        sent.add(pending.event().eventId());
    };
    private final PublishPendingEventsService service = new PublishPendingEventsService(outbox, publisher, DIRECT,
            Clock.fixed(NOW, ZoneOffset.UTC), 10, Duration.ofSeconds(1), Duration.ofMinutes(5));

    @Test
    void deliversDueEventsAndMarksThemDelivered() {
        UUID first = outbox.add(UUID.randomUUID(), 0);
        UUID second = outbox.add(UUID.randomUUID(), 0);

        assertThat(service.execute()).isEqualTo(new PublishPendingEventsResult(2, 0, 0));
        assertThat(sent).containsExactly(first, second);
        assertThat(outbox.delivered).containsOnlyKeys(first, second);
        assertThat(service.execute()).isEqualTo(new PublishPendingEventsResult(0, 0, 0));
    }

    @Test
    void failureKeepsTheEventAndHoldsBackItsAggregate() {
        UUID unit = UUID.randomUUID();
        UUID failing = outbox.add(unit, 2);
        UUID later = outbox.add(unit, 0);
        UUID otherUnit = outbox.add(UUID.randomUUID(), 0);
        broken.add(failing);

        assertThat(service.execute()).isEqualTo(new PublishPendingEventsResult(1, 1, 1));
        assertThat(sent).containsExactly(otherUnit);
        assertThat(outbox.delivered).containsOnlyKeys(otherUnit);
        assertThat(outbox.failures.get(failing).nextAttemptAt()).isEqualTo(NOW.plusSeconds(4));
        assertThat(outbox.failures.get(failing).error()).isEqualTo("RuntimeException: broker unavailable");
        assertThat(outbox.failures).doesNotContainKey(later);
    }

    @Test
    void recoveredBrokerReceivesTheSameEventId() {
        UUID event = outbox.add(UUID.randomUUID(), 0);
        broken.add(event);
        service.execute();
        broken.clear();
        outbox.makeDue(event);

        assertThat(service.execute().published()).isEqualTo(1);
        assertThat(sent).containsExactly(event);
    }

    @Test
    void backoffDoublesPerAttemptUpToTheCap() {
        UUID event = outbox.add(UUID.randomUUID(), 20);
        broken.add(event);
        service.execute();
        assertThat(outbox.failures.get(event).nextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    private record Failure(Instant nextAttemptAt, String error) {
    }

    private static final class FakeOutbox implements OutboxRepository {
        private final Map<UUID, PendingOutboxEvent> rows = new LinkedHashMap<>();
        private final Map<UUID, Instant> due = new LinkedHashMap<>();
        private final Map<UUID, Instant> delivered = new LinkedHashMap<>();
        private final Map<UUID, Failure> failures = new LinkedHashMap<>();

        UUID add(UUID aggregate, int attempts) {
            UUID id = UUID.randomUUID();
            rows.put(id, new PendingOutboxEvent(new OutboxEvent(id, "ownership.transferred", 1, UUID.randomUUID(),
                    "UNIT", aggregate, 1, Map.of(), NOW), null, attempts));
            due.put(id, NOW);
            return id;
        }

        void makeDue(UUID id) { due.put(id, NOW); }

        @Override
        public void append(OutboxEvent event) { throw new UnsupportedOperationException(); }

        @Override
        public List<PendingOutboxEvent> lockDue(Instant now, int limit) {
            return rows.values().stream()
                    .filter(p -> !delivered.containsKey(p.event().eventId()))
                    .filter(p -> !due.get(p.event().eventId()).isAfter(now))
                    .limit(limit).toList();
        }

        @Override
        public void markDelivered(UUID eventId, Instant deliveredAt) { delivered.put(eventId, deliveredAt); }

        @Override
        public void markFailed(UUID eventId, Instant nextAttemptAt, String error) {
            failures.put(eventId, new Failure(nextAttemptAt, error));
            due.put(eventId, nextAttemptAt);
        }

        @Override
        public OutboxBacklog backlog() { return new OutboxBacklog(rows.size() - delivered.size(), Optional.empty()); }
    }
}
