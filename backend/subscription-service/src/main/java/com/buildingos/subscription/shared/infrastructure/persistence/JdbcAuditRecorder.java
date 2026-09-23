package com.buildingos.subscription.shared.infrastructure.persistence;

import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditRecorder implements AuditRecorder {
    private final JdbcTemplate jdbc;
    private final JsonColumns json;

    public JdbcAuditRecorder(JdbcTemplate jdbc, JsonColumns json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void record(Entry entry) {
        jdbc.update("INSERT INTO audit_event (id, actor_user_id, action, entity_type, entity_id, before, after, occurred_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)",
                UUID.randomUUID(), entry.actorUserId(), entry.action(), entry.entityType(), entry.entityId(),
                json.write(entry.before()), json.write(entry.after()), Timestamp.from(entry.occurredAt()));
    }
}
