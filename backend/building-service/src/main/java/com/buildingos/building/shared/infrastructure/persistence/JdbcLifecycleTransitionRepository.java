package com.buildingos.building.shared.infrastructure.persistence;

import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLifecycleTransitionRepository implements LifecycleTransitionRepository {
    private final JdbcTemplate jdbc;

    public JdbcLifecycleTransitionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void append(LifecycleTransition t) {
        jdbc.update("INSERT INTO lifecycle_transition (id, entity_type, entity_id, from_status, to_status, "
                        + "actor_user_id, reason, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                t.id(), t.entityType().name(), t.entityId(), t.fromStatus(), t.toStatus(), t.actorUserId(), t.reason(),
                Timestamp.from(t.occurredAt()));
    }

    @Override
    public List<LifecycleTransition> findFor(EntityType type, UUID entityId) {
        return jdbc.query("SELECT id, entity_type, entity_id, from_status, to_status, actor_user_id, reason, occurred_at "
                + "FROM lifecycle_transition WHERE entity_type = ? AND entity_id = ? ORDER BY occurred_at, id",
                this::map, type.name(), entityId);
    }

    @Override
    public List<LifecycleTransition> list(Instant since, Instant until, String entityType, UUID actorUserId,
            int limit) {
        return jdbc.query("SELECT id, entity_type, entity_id, from_status, to_status, actor_user_id, reason, occurred_at "
                + "FROM lifecycle_transition "
                + "WHERE (CAST(? AS timestamptz) IS NULL OR occurred_at >= CAST(? AS timestamptz)) "
                + "AND (CAST(? AS timestamptz) IS NULL OR occurred_at < CAST(? AS timestamptz)) "
                + "AND (CAST(? AS varchar) IS NULL OR entity_type = CAST(? AS varchar)) "
                + "AND (CAST(? AS uuid) IS NULL OR actor_user_id = CAST(? AS uuid)) "
                + "ORDER BY occurred_at DESC, id DESC LIMIT ?",
                this::map, ts(since), ts(since), ts(until), ts(until), entityType, entityType, actorUserId, actorUserId,
                limit);
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private LifecycleTransition map(ResultSet rs, int row) throws SQLException {
        return new LifecycleTransition(rs.getObject("id", UUID.class), EntityType.valueOf(rs.getString("entity_type")),
                rs.getObject("entity_id", UUID.class), rs.getString("from_status"), rs.getString("to_status"),
                rs.getObject("actor_user_id", UUID.class), rs.getString("reason"),
                rs.getTimestamp("occurred_at").toInstant());
    }
}
