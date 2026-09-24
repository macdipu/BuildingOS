package com.buildingos.building.outbox.infrastructure.messaging;

import com.buildingos.building.outbox.application.port.out.EventPublisher;
import com.buildingos.building.shared.domain.model.OutboxEvent;
import com.buildingos.building.shared.domain.model.PendingOutboxEvent;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sends each event as a contracts/kafka/event-envelope to the topic named by its event type, keyed by aggregate id
 * so one unit's events stay ordered in a partition. Events without a request trace fall back to their event id.
 */
public class KafkaEventPublisher implements EventPublisher {
    static final String PRODUCER = "building-service";
    private static final JsonMapper JSON = JsonMapper.builder().enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .build();
    private final KafkaTemplate<String, String> kafka;
    private final Duration sendTimeout;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafka, Duration sendTimeout) {
        this.kafka = kafka;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public void publish(PendingOutboxEvent pending) {
        OutboxEvent event = pending.event();
        try {
            kafka.send(event.eventType(), event.aggregateId().toString(), JSON.writeValueAsString(envelope(pending)))
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while publishing " + event.eventId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("publishing " + event.eventId() + " failed", e);
        }
    }

    private static Map<String, Object> envelope(PendingOutboxEvent pending) {
        OutboxEvent event = pending.event();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", event.eventId().toString());
        envelope.put("eventType", event.eventType());
        envelope.put("eventVersion", event.eventVersion());
        envelope.put("occurredAt", event.occurredAt().toString());
        envelope.put("producer", PRODUCER);
        envelope.put("correlationId", pending.correlationId() == null ? event.eventId().toString()
                : pending.correlationId());
        envelope.put("buildingId", event.buildingId().toString());
        envelope.put("data", event.data());
        return envelope;
    }
}
