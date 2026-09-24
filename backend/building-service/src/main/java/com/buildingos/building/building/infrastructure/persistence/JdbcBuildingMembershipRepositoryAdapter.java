package com.buildingos.building.building.infrastructure.persistence;

import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.building.domain.model.MembershipStatus;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBuildingMembershipRepositoryAdapter implements BuildingMembershipRepository {
    private static final String COLUMNS = "id, building_id, user_id, role, status, version, created_at, updated_at, "
            + "revoked_at, revoked_by, revocation_reason";
    private final JdbcTemplate jdbc;

    public JdbcBuildingMembershipRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(BuildingMembership m) {
        jdbc.update("INSERT INTO building_membership (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                m.id(), m.buildingId(), m.userId(), m.role().name(), m.status().name(), m.version(),
                Timestamp.from(m.createdAt()), Timestamp.from(m.updatedAt()), timestamp(m.revokedAt()), m.revokedBy(),
                m.revocationReason());
    }

    @Override
    public void update(BuildingMembership m) {
        int updated = jdbc.update("UPDATE building_membership SET status = ?, version = ?, updated_at = ?, "
                        + "revoked_at = ?, revoked_by = ?, revocation_reason = ? WHERE id = ? AND version = ?",
                m.status().name(), m.version(), Timestamp.from(m.updatedAt()), timestamp(m.revokedAt()),
                m.revokedBy(), m.revocationReason(), m.id(), m.version() - 1);
        if (updated != 1) {
            throw new IllegalStateException("Membership " + m.id() + " changed outside the building lock");
        }
    }

    @Override
    public List<BuildingMembership> findByBuilding(UUID buildingId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_membership WHERE building_id = ? "
                + "ORDER BY created_at, id", this::map, buildingId);
    }

    @Override
    public List<BuildingMembership> findByBuilding(UUID buildingId, int page, int size) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_membership WHERE building_id = ? "
                + "ORDER BY created_at, id LIMIT ? OFFSET ?", this::map, buildingId, size, (long) page * size);
    }

    @Override
    public long countByBuilding(UUID buildingId) {
        return jdbc.queryForObject("SELECT count(*) FROM building_membership WHERE building_id = ?", Long.class,
                buildingId);
    }

    @Override
    public Optional<BuildingMembership> findInBuilding(UUID buildingId, UUID membershipId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_membership WHERE building_id = ? AND id = ?",
                this::map, buildingId, membershipId).stream().findFirst();
    }

    @Override
    public Optional<BuildingMembership> find(UUID buildingId, UUID userId, BuildingRole role) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_membership WHERE building_id = ? AND user_id = ? "
                + "AND role = ?", this::map, buildingId, userId, role.name()).stream().findFirst();
    }

    @Override
    public List<BuildingMembership> findActive(UUID buildingId, UUID userId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_membership WHERE building_id = ? AND user_id = ? "
                + "AND status = 'ACTIVE'", this::map, buildingId, userId);
    }

    @Override
    public long countActiveAdmins(UUID buildingId) {
        return jdbc.queryForObject("SELECT count(*) FROM building_membership WHERE building_id = ? "
                + "AND role = 'BUILDING_ADMIN' AND status = 'ACTIVE'", Long.class, buildingId);
    }

    private static Timestamp timestamp(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }

    private BuildingMembership map(ResultSet rs, int row) throws SQLException {
        Timestamp revokedAt = rs.getTimestamp("revoked_at");
        return new BuildingMembership(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getObject("user_id", UUID.class), BuildingRole.valueOf(rs.getString("role")),
                MembershipStatus.valueOf(rs.getString("status")), rs.getLong("version"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                revokedAt == null ? null : revokedAt.toInstant(), rs.getObject("revoked_by", UUID.class),
                rs.getString("revocation_reason"));
    }
}
