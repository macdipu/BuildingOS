package com.buildingos.backoffice.supportsession.infrastructure.persistence;

import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalFilter;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalStatus;
import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcElevatedApprovalRequestRepositoryAdapter implements ElevatedApprovalRequestRepository {
    private static final String COLUMNS = "e.id, e.support_session_id, e.requested_scope, e.status, e.approved_by, "
            + "e.requested_at, e.decided_at, e.decision_reason";
    private static final String FROM = " FROM elevated_approval_request e";

    private final JdbcTemplate jdbc;

    public JdbcElevatedApprovalRequestRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(ElevatedApprovalRequest r) {
        jdbc.update("INSERT INTO elevated_approval_request (id, support_session_id, requested_scope, status, "
                        + "approved_by, requested_at, decided_at, decision_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                r.id(), r.supportSessionId(), r.requestedScope().name(), r.status().name(), r.decidedBy(),
                ts(r.requestedAt()), ts(r.decidedAt()), r.decisionReason());
    }

    @Override
    public void update(ElevatedApprovalRequest r) {
        jdbc.update("UPDATE elevated_approval_request SET status = ?, approved_by = ?, decided_at = ?, "
                + "decision_reason = ? WHERE id = ?", r.status().name(), r.decidedBy(), ts(r.decidedAt()),
                r.decisionReason(), r.id());
    }

    @Override
    public Optional<ElevatedApprovalRequest> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + FROM + " WHERE e.id = ?", this::map, id).stream().findFirst();
    }

    @Override
    public Optional<ElevatedApprovalRequest> findByIdForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + FROM + " WHERE e.id = ? FOR UPDATE", this::map, id).stream()
                .findFirst();
    }

    @Override
    public List<ElevatedApprovalRequest> findBySession(UUID supportSessionId) {
        return jdbc.query("SELECT " + COLUMNS + FROM + " WHERE e.support_session_id = ? ORDER BY e.requested_at, e.id",
                this::map, supportSessionId);
    }

    @Override
    public List<ElevatedApprovalRequest> search(ElevatedApprovalFilter filter, int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String where = where(filter, args);
        args.add(limit);
        args.add(offset);
        return jdbc.query("SELECT " + COLUMNS + FROM + where + " ORDER BY e.requested_at DESC, e.id LIMIT ? OFFSET ?",
                this::map, args.toArray());
    }

    @Override
    public long count(ElevatedApprovalFilter filter) {
        List<Object> args = new ArrayList<>();
        String where = where(filter, args);
        Long total = jdbc.queryForObject("SELECT count(*)" + FROM + where, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    private static String where(ElevatedApprovalFilter filter, List<Object> args) {
        String join = "";
        List<String> clauses = new ArrayList<>();
        if (filter.status() != null) {
            clauses.add("e.status = ?");
            args.add(filter.status().name());
        }
        if (filter.sessionOwnerUserId() != null) {
            join = " JOIN support_session s ON s.id = e.support_session_id";
            clauses.add("s.platform_user_id = ?");
            args.add(filter.sessionOwnerUserId());
        }
        return join + (clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses));
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private ElevatedApprovalRequest map(ResultSet rs, int row) throws SQLException {
        return new ElevatedApprovalRequest(rs.getObject("id", UUID.class),
                rs.getObject("support_session_id", UUID.class), SupportScope.valueOf(rs.getString("requested_scope")),
                ElevatedApprovalStatus.valueOf(rs.getString("status")), rs.getObject("approved_by", UUID.class),
                instant(rs, "requested_at"), instant(rs, "decided_at"), rs.getString("decision_reason"));
    }
}
