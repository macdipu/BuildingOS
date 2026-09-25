package com.buildingos.auth.auth.infrastructure.persistence.repository;

import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditAction;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import com.buildingos.auth.auth.domain.repository.PlatformRoleAuditRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPlatformRoleAuditRepositoryAdapter implements PlatformRoleAuditRepository {
    private static final RowMapper<PlatformRoleAuditEntry> ROW = (rs, n) -> new PlatformRoleAuditEntry(
            rs.getObject("id", UUID.class),
            rs.getObject("actor_user_id", UUID.class),
            rs.getObject("target_user_id", UUID.class),
            PlatformRole.valueOf(rs.getString("role")),
            PlatformRoleAuditAction.valueOf(rs.getString("action")),
            rs.getTimestamp("occurred_at").toInstant());

    private final JdbcTemplate jdbc;

    public JdbcPlatformRoleAuditRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(PlatformRoleAuditEntry entry) {
        jdbc.update("""
                INSERT INTO platform_role_audit (id, actor_user_id, target_user_id, role, action, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, entry.id(), entry.actorUserId(), entry.targetUserId(), entry.role().name(),
                entry.action().name(), Timestamp.from(entry.occurredAt()));
    }

    @Override
    public List<PlatformRoleAuditEntry> list(Instant since, Instant until, UUID actorUserId, int limit) {
        return jdbc.query("""
                SELECT id, actor_user_id, target_user_id, role, action, occurred_at FROM platform_role_audit
                WHERE (CAST(? AS timestamptz) IS NULL OR occurred_at >= CAST(? AS timestamptz))
                  AND (CAST(? AS timestamptz) IS NULL OR occurred_at < CAST(? AS timestamptz))
                  AND (CAST(? AS uuid) IS NULL OR actor_user_id = CAST(? AS uuid))
                ORDER BY occurred_at DESC, id DESC
                LIMIT ?
                """, ROW, ts(since), ts(since), ts(until), ts(until), actorUserId, actorUserId, limit);
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
