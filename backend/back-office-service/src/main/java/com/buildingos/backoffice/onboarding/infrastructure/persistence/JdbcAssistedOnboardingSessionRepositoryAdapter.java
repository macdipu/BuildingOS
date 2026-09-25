package com.buildingos.backoffice.onboarding.infrastructure.persistence;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingScope;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionFilter;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
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
public class JdbcAssistedOnboardingSessionRepositoryAdapter implements AssistedOnboardingSessionRepository {
    private static final String COLUMNS = "id, building_id, assigned_agent_user_id, requested_by_user_id, status, "
            + "access_scope, reason, started_at, expires_at, completed_at, notes";
    private static final String ACTIVE = OnboardingSessionStatus.ACTIVE.stream()
            .map(status -> "'" + status.name() + "'").sorted().collect(Collectors.joining(", ", "(", ")"));

    private final JdbcTemplate jdbc;

    public JdbcAssistedOnboardingSessionRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(AssistedOnboardingSession s) {
        jdbc.update("INSERT INTO assisted_onboarding_session (" + COLUMNS + ") "
                        + "VALUES (?, ?, ?, ?, ?, ?::text[], ?, ?, ?, ?, ?)",
                s.id(), s.buildingId(), s.assignedAgentUserId(), s.requestedByUserId(), s.status().name(),
                scopeLiteral(s.accessScope()), s.reason(), ts(s.startedAt()), ts(s.expiresAt()), ts(s.completedAt()),
                s.notes());
    }

    @Override
    public void update(AssistedOnboardingSession s) {
        jdbc.update("UPDATE assisted_onboarding_session SET status = ?, completed_at = ? WHERE id = ?",
                s.status().name(), ts(s.completedAt()), s.id());
    }

    @Override
    public Optional<AssistedOnboardingSession> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM assisted_onboarding_session WHERE id = ?", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<AssistedOnboardingSession> findByIdForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM assisted_onboarding_session WHERE id = ? FOR UPDATE",
                this::map, id).stream().findFirst();
    }

    @Override
    public List<AssistedOnboardingSession> lockDueForExpiry(Instant now, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM assisted_onboarding_session WHERE status IN " + ACTIVE
                + " AND expires_at <= ? ORDER BY expires_at, id LIMIT ? FOR UPDATE SKIP LOCKED",
                this::map, ts(now), limit);
    }

    @Override
    public List<AssistedOnboardingSession> search(OnboardingSessionFilter filter, int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String where = where(filter, args);
        args.add(limit);
        args.add(offset);
        return jdbc.query("SELECT " + COLUMNS + " FROM assisted_onboarding_session" + where
                + " ORDER BY started_at DESC, id LIMIT ? OFFSET ?", this::map, args.toArray());
    }

    @Override
    public long count(OnboardingSessionFilter filter) {
        List<Object> args = new ArrayList<>();
        String where = where(filter, args);
        Long total = jdbc.queryForObject("SELECT count(*) FROM assisted_onboarding_session" + where, Long.class,
                args.toArray());
        return total == null ? 0 : total;
    }

    private static String where(OnboardingSessionFilter filter, List<Object> args) {
        List<String> clauses = new ArrayList<>();
        if (filter.status() != null) {
            clauses.add("status = ?");
            args.add(filter.status().name());
        }
        if (filter.buildingId() != null) {
            clauses.add("building_id = ?");
            args.add(filter.buildingId());
        }
        if (filter.agentUserId() != null) {
            clauses.add("assigned_agent_user_id = ?");
            args.add(filter.agentUserId());
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    /** Enum names are [A-Z_] only, so the array literal needs no quoting. */
    private static String scopeLiteral(List<OnboardingScope> scopes) {
        return scopes.stream().map(Enum::name).collect(Collectors.joining(",", "{", "}"));
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private AssistedOnboardingSession map(ResultSet rs, int row) throws SQLException {
        String[] scopes = (String[]) rs.getArray("access_scope").getArray();
        return new AssistedOnboardingSession(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getObject("assigned_agent_user_id", UUID.class), rs.getObject("requested_by_user_id", UUID.class),
                OnboardingSessionStatus.valueOf(rs.getString("status")),
                Arrays.stream(scopes).map(OnboardingScope::valueOf).toList(), rs.getString("reason"),
                instant(rs, "started_at"), instant(rs, "expires_at"), instant(rs, "completed_at"),
                rs.getString("notes"));
    }
}
