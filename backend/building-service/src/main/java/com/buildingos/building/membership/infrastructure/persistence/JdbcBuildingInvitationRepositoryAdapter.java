package com.buildingos.building.membership.infrastructure.persistence;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.membership.domain.model.BuildingInvitation;
import com.buildingos.building.membership.domain.model.InvitationStatus;
import com.buildingos.building.membership.domain.repository.BuildingInvitationRepository;
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
public class JdbcBuildingInvitationRepositoryAdapter implements BuildingInvitationRepository {
    private static final String COLUMNS = "id, building_id, phone, role, status, created_by, reason, created_at, "
            + "expires_at, version, claimed_user_id, claimed_at, revoked_by, revoked_at, revocation_reason";
    private final JdbcTemplate jdbc;

    public JdbcBuildingInvitationRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(BuildingInvitation i) {
        jdbc.update("INSERT INTO building_invitation (" + COLUMNS + ") "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                i.id(), i.buildingId(), i.phone().value(), i.role().name(), i.status().name(), i.createdBy(),
                i.reason(), Timestamp.from(i.createdAt()), Timestamp.from(i.expiresAt()), i.version(),
                i.claimedUserId(), timestamp(i.claimedAt()), i.revokedBy(), timestamp(i.revokedAt()),
                i.revocationReason());
    }

    @Override
    public void update(BuildingInvitation i) {
        int updated = jdbc.update("UPDATE building_invitation SET status = ?, version = ?, claimed_user_id = ?, "
                        + "claimed_at = ?, revoked_by = ?, revoked_at = ?, revocation_reason = ? "
                        + "WHERE id = ? AND version = ?",
                i.status().name(), i.version(), i.claimedUserId(), timestamp(i.claimedAt()), i.revokedBy(),
                timestamp(i.revokedAt()), i.revocationReason(), i.id(), i.version() - 1);
        if (updated != 1) {
            throw new IllegalStateException("Invitation " + i.id() + " changed outside the building lock");
        }
    }

    @Override
    public Optional<BuildingInvitation> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_invitation WHERE id = ?", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<BuildingInvitation> findInBuilding(UUID buildingId, UUID invitationId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_invitation WHERE building_id = ? AND id = ?",
                this::map, buildingId, invitationId).stream().findFirst();
    }

    @Override
    public Optional<BuildingInvitation> findPending(UUID buildingId, ContactPhone phone, BuildingRole role) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_invitation WHERE building_id = ? AND phone = ? "
                + "AND role = ? AND status = 'PENDING'", this::map, buildingId, phone.value(), role.name())
                .stream().findFirst();
    }

    @Override
    public List<BuildingInvitation> findByBuilding(UUID buildingId, int page, int size) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_invitation WHERE building_id = ? "
                + "ORDER BY created_at DESC, id LIMIT ? OFFSET ?", this::map, buildingId, size, (long) page * size);
    }

    @Override
    public long countByBuilding(UUID buildingId) {
        return jdbc.queryForObject("SELECT count(*) FROM building_invitation WHERE building_id = ?", Long.class,
                buildingId);
    }

    @Override
    public List<BuildingInvitation> findLiveForPhone(ContactPhone phone, Instant now) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_invitation WHERE phone = ? AND status = 'PENDING' "
                + "AND expires_at > ? ORDER BY created_at, id", this::map, phone.value(), Timestamp.from(now));
    }

    private static Timestamp timestamp(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }

    private static Instant instant(Timestamp at) {
        return at == null ? null : at.toInstant();
    }

    private BuildingInvitation map(ResultSet rs, int row) throws SQLException {
        return new BuildingInvitation(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                new ContactPhone(rs.getString("phone")), BuildingRole.valueOf(rs.getString("role")),
                InvitationStatus.valueOf(rs.getString("status")), rs.getObject("created_by", UUID.class),
                rs.getString("reason"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(), rs.getLong("version"),
                rs.getObject("claimed_user_id", UUID.class), instant(rs.getTimestamp("claimed_at")),
                rs.getObject("revoked_by", UUID.class), instant(rs.getTimestamp("revoked_at")),
                rs.getString("revocation_reason"));
    }
}
