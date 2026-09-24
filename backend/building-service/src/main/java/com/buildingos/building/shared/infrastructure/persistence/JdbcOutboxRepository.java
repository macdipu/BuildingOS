package com.buildingos.building.shared.infrastructure.persistence;

import com.buildingos.building.shared.domain.model.OutboxBacklog;
import com.buildingos.building.shared.domain.model.OutboxEvent;
import com.buildingos.building.shared.domain.model.PendingOutboxEvent;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Pending rows are due immediately; the correlation id is the request trace id from platform-web's MDC. */
@Repository
public class JdbcOutboxRepository implements OutboxRepository {
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
    private static final TypeReference<Map<String, Object>> PAYLOAD = new TypeReference<>() { };
    private static final int MAX_ERROR_LENGTH = 500;
    private final JdbcTemplate jdbc;

    public JdbcOutboxRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void append(OutboxEvent e) {
        jdbc.update("INSERT INTO building_outbox (event_id, event_type, event_version, building_id, aggregate_type, "
                        + "aggregate_id, aggregate_revision, correlation_id, payload, occurred_at, next_attempt_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)",
                e.eventId(), e.eventType(), e.eventVersion(), e.buildingId(), e.aggregateType(), e.aggregateId(),
                e.aggregateRevision(), MDC.get("traceId"), JSON.writeValueAsString(e.data()),
                Timestamp.from(e.occurredAt()), Timestamp.from(e.occurredAt()));
    }

    @Override
    public List<PendingOutboxEvent> lockDue(Instant now, int limit) {
        return jdbc.query("SELECT event_id, event_type, event_version, building_id, aggregate_type, aggregate_id, "
                        + "aggregate_revision, correlation_id, payload::text AS payload, occurred_at, attempts "
                        + "FROM building_outbox WHERE delivered_at IS NULL AND next_attempt_at <= ? "
                        + "ORDER BY occurred_at, event_id LIMIT ? FOR UPDATE SKIP LOCKED",
                (rs, i) -> new PendingOutboxEvent(new OutboxEvent(rs.getObject("event_id", UUID.class),
                        rs.getString("event_type"), rs.getInt("event_version"),
                        rs.getObject("building_id", UUID.class), rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class), rs.getLong("aggregate_revision"),
                        JSON.readValue(rs.getString("payload"), PAYLOAD), rs.getTimestamp("occurred_at").toInstant()),
                        rs.getString("correlation_id"), rs.getInt("attempts")),
                Timestamp.from(now), limit);
    }

    @Override
    public void markDelivered(UUID eventId, Instant deliveredAt) {
        jdbc.update("UPDATE building_outbox SET delivered_at = ?, last_error = NULL WHERE event_id = ?",
                Timestamp.from(deliveredAt), eventId);
    }

    @Override
    public void markFailed(UUID eventId, Instant nextAttemptAt, String error) {
        String bounded = error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
        jdbc.update("UPDATE building_outbox SET attempts = attempts + 1, next_attempt_at = ?, last_error = ? "
                + "WHERE event_id = ?", Timestamp.from(nextAttemptAt), bounded, eventId);
    }

    @Override
    public OutboxBacklog backlog() {
        return jdbc.queryForObject("SELECT count(*) AS pending, min(occurred_at) AS oldest FROM building_outbox "
                        + "WHERE delivered_at IS NULL",
                (rs, i) -> new OutboxBacklog(rs.getLong("pending"),
                        Optional.ofNullable(rs.getTimestamp("oldest")).map(Timestamp::toInstant)));
    }
}
