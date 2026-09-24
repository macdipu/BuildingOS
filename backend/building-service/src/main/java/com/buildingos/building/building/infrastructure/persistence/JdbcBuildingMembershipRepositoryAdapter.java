package com.buildingos.building.building.infrastructure.persistence;

import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.building.domain.model.MembershipStatus;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBuildingMembershipRepositoryAdapter implements BuildingMembershipRepository {
    private final JdbcTemplate jdbc;

    public JdbcBuildingMembershipRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(BuildingMembership m) {
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                m.id(), m.buildingId(), m.userId(), m.role().name(), m.status().name(), Timestamp.from(m.createdAt()));
    }

    @Override
    public List<BuildingMembership> findByBuilding(UUID buildingId) {
        return jdbc.query("SELECT id, building_id, user_id, role, status, created_at FROM building_membership "
                + "WHERE building_id = ? ORDER BY created_at, id", this::map, buildingId);
    }

    @Override
    public long countActiveAdmins(UUID buildingId) {
        return jdbc.queryForObject("SELECT count(*) FROM building_membership WHERE building_id = ? "
                + "AND role = 'BUILDING_ADMIN' AND status = 'ACTIVE'", Long.class, buildingId);
    }

    private BuildingMembership map(ResultSet rs, int row) throws SQLException {
        return new BuildingMembership(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getObject("user_id", UUID.class), BuildingRole.valueOf(rs.getString("role")),
                MembershipStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant());
    }
}
