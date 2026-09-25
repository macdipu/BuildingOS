package com.buildingos.backoffice.supportsession.infrastructure.persistence;

import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionFilter;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSupportSessionRepositoryAdapter implements SupportSessionRepository {
    private static final String COLUMNS = "id, platform_user_id, target_user_id, building_id, reason, "
            + "permission_scope, started_at, expires_at, ended_at";

    private final JdbcTemplate jdbc;

    public JdbcSupportSessionRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(SupportSession s) {
        jdbc.update("INSERT INTO support_session (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?::text[], ?, ?, ?)",
                s.id(), s.platformUserId(), s.targetUserId(), s.buildingId(), s.reason(),
                scopeLiteral(s.permissionScope()), ts(s.startedAt()), ts(s.expiresAt()), ts(s.endedAt()));
    }

    @Override
    public void update(SupportSession s) {
        jdbc.update("UPDATE support_session SET permission_scope = ?::text[], ended_at = ? WHERE id = ?",
                scopeLiteral(s.permissionScope()), ts(s.endedAt()), s.id());
    }

    @Override
    public Optional<SupportSession> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM support_session WHERE id = ?", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<SupportSession> findByIdForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM support_session WHERE id = ? FOR UPDATE", this::map, id)
                .stream().findFirst();
    }

    @Override
    public List<SupportSession> lockDueForExpiry(Instant now, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM support_session WHERE ended_at IS NULL AND expires_at <= ? "
                + "ORDER BY expires_at, id LIMIT ? FOR UPDATE SKIP LOCKED", this::map, ts(now), limit);
    }

    @Override
    public List<SupportSession> search(SupportSessionFilter filter, int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String where = where(filter, args);
        args.add(limit);
        args.add(offset);
        return jdbc.query("SELECT " + COLUMNS + " FROM support_session" + where
                + " ORDER BY started_at DESC, id LIMIT ? OFFSET ?", this::map, args.toArray());
    }

    @Override
    public long count(SupportSessionFilter filter) {
        List<Object> args = new ArrayList<>();
        String where = where(filter, args);
        Long total = jdbc.queryForObject("SELECT count(*) FROM support_session" + where, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    private static String where(SupportSessionFilter filter, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (filter.active() != null) {
            clauses.add(filter.active() ? "ended_at IS NULL" : "ended_at IS NOT NULL");
        }
        if (filter.platformUserId() != null) {
            clauses.add("platform_user_id = ?");
            args.add(filter.platformUserId());
        }
        if (filter.targetUserId() != null) {
            clauses.add("target_user_id = ?");
            args.add(filter.targetUserId());
        }
        if (filter.buildingId() != null) {
            clauses.add("building_id = ?");
            args.add(filter.buildingId());
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    /** Enum names are [A-Z_] only, so the array literal needs no quoting. */
    private static String scopeLiteral(List<SupportScope> scopes) {
        return scopes.stream().map(Enum::name).collect(Collectors.joining(",", "{", "}"));
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private SupportSession map(ResultSet rs, int row) throws SQLException {
        String[] scopes = (String[]) rs.getArray("permission_scope").getArray();
        return new SupportSession(rs.getObject("id", UUID.class), rs.getObject("platform_user_id", UUID.class),
                rs.getObject("target_user_id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getString("reason"), Arrays.stream(scopes).map(SupportScope::valueOf).toList(),
                instant(rs, "started_at"), instant(rs, "expires_at"), instant(rs, "ended_at"));
    }
}
