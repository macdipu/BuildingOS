package com.buildingos.building.shared.infrastructure.persistence;

import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Append-only; the request trace id comes from platform-web's correlation MDC. */
@Repository
public class JdbcAuditRepository implements AuditRepository {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<Map<String, String>> FIELDS = new TypeReference<>() {};
    private final JdbcTemplate jdbc;

    public JdbcAuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void append(AuditEntry e) {
        jdbc.update("INSERT INTO building_audit (id, building_id, actor_user_id, action, entity_type, entity_id, "
                        + "reason, before_data, after_data, trace_id, occurred_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?)",
                e.id(), e.buildingId(), e.actorUserId(), e.action(), e.entityType(), e.entityId(), e.reason(),
                JSON.writeValueAsString(e.before()), JSON.writeValueAsString(e.after()), MDC.get("traceId"),
                Timestamp.from(e.occurredAt()));
    }

    @Override
    public List<AuditEntry> findByEntity(UUID buildingId, String entityType, UUID entityId) {
        return jdbc.query("SELECT id, building_id, actor_user_id, action, entity_type, entity_id, reason, "
                + "before_data, after_data, occurred_at FROM building_audit WHERE building_id = ? AND entity_type = ? "
                + "AND entity_id = ? ORDER BY occurred_at, id", this::map, buildingId, entityType, entityId);
    }

    private AuditEntry map(ResultSet rs, int row) throws SQLException {
        return new AuditEntry(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getObject("actor_user_id", UUID.class), rs.getString("action"), rs.getString("entity_type"),
                rs.getObject("entity_id", UUID.class), rs.getString("reason"),
                JSON.readValue(rs.getString("before_data"), FIELDS), JSON.readValue(rs.getString("after_data"), FIELDS),
                rs.getTimestamp("occurred_at").toInstant());
    }
}
