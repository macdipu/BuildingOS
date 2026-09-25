package com.buildingos.subscription.audit.infrastructure.persistence;

import com.buildingos.subscription.audit.domain.model.AuditRecord;
import com.buildingos.subscription.audit.domain.repository.AuditEventRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditEventRepositoryAdapter implements AuditEventRepository {
    private static final RowMapper<AuditRecord> ROW = (rs, n) -> new AuditRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("actor_user_id", UUID.class),
            rs.getString("action"),
            rs.getString("entity_type"),
            rs.getString("entity_id"),
            rs.getTimestamp("occurred_at").toInstant());

    private final JdbcTemplate jdbc;

    public JdbcAuditEventRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AuditRecord> list(Instant since, Instant until, String entityType, UUID actorUserId, int limit) {
        return jdbc.query("""
                SELECT id, actor_user_id, action, entity_type, entity_id, occurred_at FROM audit_event
                WHERE (CAST(? AS timestamptz) IS NULL OR occurred_at >= CAST(? AS timestamptz))
                  AND (CAST(? AS timestamptz) IS NULL OR occurred_at < CAST(? AS timestamptz))
                  AND (CAST(? AS varchar) IS NULL OR entity_type = CAST(? AS varchar))
                  AND (CAST(? AS uuid) IS NULL OR actor_user_id = CAST(? AS uuid))
                ORDER BY occurred_at DESC, id DESC
                LIMIT ?
                """, ROW, ts(since), ts(since), ts(until), ts(until), entityType, entityType, actorUserId,
                actorUserId, limit);
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
