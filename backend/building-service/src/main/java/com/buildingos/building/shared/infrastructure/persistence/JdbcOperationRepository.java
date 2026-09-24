package com.buildingos.building.shared.infrastructure.persistence;

import com.buildingos.building.shared.domain.model.RecordedOperation;
import com.buildingos.building.shared.domain.repository.OperationRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperationRepository implements OperationRepository {
    private final JdbcTemplate jdbc;

    public JdbcOperationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<RecordedOperation> find(UUID actorUserId, String action, UUID operationId) {
        return jdbc.query("SELECT actor_user_id, action, operation_id, building_id, request_fingerprint, "
                        + "result_entity_id, result_version, created_at FROM building_operation "
                        + "WHERE actor_user_id = ? AND action = ? AND operation_id = ?",
                this::map, actorUserId, action, operationId).stream().findFirst();
    }

    @Override
    public void insert(RecordedOperation o) {
        jdbc.update("INSERT INTO building_operation (actor_user_id, action, operation_id, building_id, "
                        + "request_fingerprint, result_entity_id, result_version, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                o.actorUserId(), o.action(), o.operationId(), o.buildingId(), o.requestFingerprint(),
                o.resultEntityId(), o.resultVersion(), Timestamp.from(o.createdAt()));
    }

    private RecordedOperation map(ResultSet rs, int row) throws SQLException {
        return new RecordedOperation(rs.getObject("actor_user_id", UUID.class), rs.getString("action"),
                rs.getObject("operation_id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getString("request_fingerprint"), rs.getObject("result_entity_id", UUID.class),
                rs.getLong("result_version"), rs.getTimestamp("created_at").toInstant());
    }
}
