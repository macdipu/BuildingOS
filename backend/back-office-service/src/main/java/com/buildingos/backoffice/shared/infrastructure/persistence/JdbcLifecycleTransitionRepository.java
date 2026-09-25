package com.buildingos.backoffice.shared.infrastructure.persistence;

import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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
    public List<LifecycleTransition> list(Instant since, Instant until, EntityType type, UUID actorUserId, int limit) {
        StringBuilder sql = new StringBuilder("SELECT id, entity_type, entity_id, from_status, to_status, actor_user_id, "
                + "reason, occurred_at FROM lifecycle_transition WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (since != null) {
            sql.append(" AND occurred_at >= ?");
            args.add(Timestamp.from(since));
        }
        if (until != null) {
            sql.append(" AND occurred_at < ?");
            args.add(Timestamp.from(until));
        }
        if (type != null) {
            sql.append(" AND entity_type = ?");
            args.add(type.name());
        }
        if (actorUserId != null) {
            sql.append(" AND actor_user_id = ?");
            args.add(actorUserId);
        }
        sql.append(" ORDER BY occurred_at DESC, id DESC LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), this::map, args.toArray());
    }

    private LifecycleTransition map(ResultSet rs, int row) throws SQLException {
        return new LifecycleTransition(rs.getObject("id", UUID.class), EntityType.valueOf(rs.getString("entity_type")),
                rs.getObject("entity_id", UUID.class), rs.getString("from_status"), rs.getString("to_status"),
                rs.getObject("actor_user_id", UUID.class), rs.getString("reason"),
                rs.getTimestamp("occurred_at").toInstant());
    }
}
