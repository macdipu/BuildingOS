package com.buildingos.building.shared.infrastructure.persistence;

import com.buildingos.building.shared.domain.model.OutboxEvent;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import java.sql.Timestamp;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Pending rows are due immediately; the correlation id is the request trace id from platform-web's MDC. */
@Repository
public class JdbcOutboxRepository implements OutboxRepository {
    private static final JsonMapper JSON = JsonMapper.builder().build();
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
}
