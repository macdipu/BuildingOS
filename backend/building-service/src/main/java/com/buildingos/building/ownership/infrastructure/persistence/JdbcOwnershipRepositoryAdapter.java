package com.buildingos.building.ownership.infrastructure.persistence;

import com.buildingos.building.ownership.domain.model.OwnedProperty;
import com.buildingos.building.ownership.domain.model.OwnershipChange;
import com.buildingos.building.ownership.domain.model.OwnershipHistory;
import com.buildingos.building.ownership.domain.model.OwnershipPeriod;
import com.buildingos.building.ownership.domain.model.OwnershipTransfer;
import com.buildingos.building.ownership.domain.model.Share;
import com.buildingos.building.ownership.domain.model.UnitOwnership;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.unit.domain.model.UnitType;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOwnershipRepositoryAdapter implements OwnershipRepository {
    private static final String PERIOD_COLUMNS = "id, building_id, unit_id, owner_user_id, share, start_at, "
            + "start_revision, end_at, end_revision, notes, created_by";
    private static final String TRANSFER_COLUMNS = "id, building_id, unit_id, source_owner_user_id, recipient_user_id, "
            + "share, effective_date, effective_at, revision, actor_user_id, reason, reference";
    private static final String PROPERTIES = " FROM ownership_period p JOIN building_unit u ON u.id = p.unit_id "
            + "JOIN building b ON b.id = p.building_id JOIN building_floor f ON f.id = u.floor_id "
            + "WHERE p.owner_user_id = ? AND p.end_revision IS NULL AND EXISTS (SELECT 1 FROM building_membership m "
            + "WHERE m.building_id = p.building_id AND m.user_id = p.owner_user_id AND m.status = 'ACTIVE')";
    private final JdbcTemplate jdbc;

    public JdbcOwnershipRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<UnitOwnership> lockUnit(UUID buildingId, UUID unitId) {
        return jdbc.query("SELECT ownership_revision FROM building_unit WHERE building_id = ? AND id = ? FOR UPDATE",
                (rs, row) -> rs.getLong(1), buildingId, unitId).stream().findFirst()
                .map(revision -> new UnitOwnership(buildingId, unitId, revision, jdbc.query("SELECT " + PERIOD_COLUMNS
                        + " FROM ownership_period WHERE unit_id = ? AND end_revision IS NULL ORDER BY start_revision, id",
                        this::period, unitId)));
    }

    @Override
    public void apply(OwnershipChange change) {
        UUID unitId = change.opened().isEmpty() ? change.closed().get(0).unitId() : change.opened().get(0).unitId();
        int updated = jdbc.update("UPDATE building_unit SET ownership_revision = ? WHERE id = ? "
                + "AND ownership_revision = ?", change.revision(), unitId, change.previousRevision());
        if (updated != 1) {
            throw new IllegalStateException("Unit " + unitId + " ownership changed outside its lock");
        }
        for (var closed : change.closed()) {
            int ended = jdbc.update("UPDATE ownership_period SET end_at = ?, end_revision = ? WHERE id = ? "
                    + "AND end_revision IS NULL", Timestamp.from(closed.endAt()), closed.endRevision(), closed.id());
            if (ended != 1) {
                throw new IllegalStateException("Ownership period " + closed.id() + " was already closed");
            }
        }
        for (var p : change.opened()) {
            jdbc.update("INSERT INTO ownership_period (" + PERIOD_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    p.id(), p.buildingId(), p.unitId(), p.ownerUserId(), p.share().percent(),
                    Timestamp.from(p.startAt()), p.startRevision(), null, null, p.notes(), p.createdBy());
        }
        var t = change.transfer();
        if (t != null) {
            jdbc.update("INSERT INTO ownership_transfer (" + TRANSFER_COLUMNS + ") "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    t.id(), t.buildingId(), t.unitId(), t.sourceOwnerUserId(), t.recipientUserId(), t.share().percent(),
                    Date.valueOf(t.effectiveDate()), Timestamp.from(t.effectiveAt()), t.revision(), t.actorUserId(),
                    t.reason(), t.reference());
        }
    }

    @Override
    public Optional<OwnershipHistory> history(UUID buildingId, UUID unitId, UUID ownerUserId) {
        var revision = jdbc.query("SELECT ownership_revision FROM building_unit WHERE building_id = ? AND id = ?",
                (rs, row) -> rs.getLong(1), buildingId, unitId).stream().findFirst();
        if (revision.isEmpty()) {
            return Optional.empty();
        }
        List<OwnershipPeriod> periods = ownerUserId == null
                ? jdbc.query("SELECT " + PERIOD_COLUMNS + " FROM ownership_period WHERE building_id = ? AND unit_id = ? "
                        + "ORDER BY start_revision, id", this::period, buildingId, unitId)
                : jdbc.query("SELECT " + PERIOD_COLUMNS + " FROM ownership_period WHERE building_id = ? AND unit_id = ? "
                        + "AND owner_user_id = ? ORDER BY start_revision, id", this::period, buildingId, unitId,
                        ownerUserId);
        List<OwnershipTransfer> transfers = ownerUserId == null
                ? jdbc.query("SELECT " + TRANSFER_COLUMNS + " FROM ownership_transfer WHERE building_id = ? "
                        + "AND unit_id = ? ORDER BY revision", this::transfer, buildingId, unitId)
                : jdbc.query("SELECT " + TRANSFER_COLUMNS + " FROM ownership_transfer WHERE building_id = ? "
                        + "AND unit_id = ? AND ? IN (source_owner_user_id, recipient_user_id) ORDER BY revision",
                        this::transfer, buildingId, unitId, ownerUserId);
        return Optional.of(new OwnershipHistory(revision.get(), periods, transfers));
    }

    @Override
    public Optional<UnitOwnership> current(UUID buildingId, UUID unitId) {
        return jdbc.query("SELECT ownership_revision FROM building_unit WHERE building_id = ? AND id = ?",
                (rs, row) -> rs.getLong(1), buildingId, unitId).stream().findFirst()
                .map(revision -> new UnitOwnership(buildingId, unitId, revision, jdbc.query("SELECT " + PERIOD_COLUMNS
                        + " FROM ownership_period WHERE unit_id = ? AND end_revision IS NULL ORDER BY start_revision, id",
                        this::period, unitId)));
    }

    @Override
    public List<OwnedProperty> propertiesOf(UUID userId, int page, int size) {
        return jdbc.query("SELECT b.id AS building_id, b.name AS building_name, u.id AS unit_id, u.number, "
                + "f.label AS floor_label, u.unit_type, u.area_sqft, p.share, p.start_at" + PROPERTIES
                + " ORDER BY b.name, b.id, u.normalized_number, u.id LIMIT ? OFFSET ?", (rs, row) -> new OwnedProperty(
                        rs.getObject("building_id", UUID.class), rs.getString("building_name"),
                        rs.getObject("unit_id", UUID.class), rs.getString("number"), rs.getString("floor_label"),
                        UnitType.valueOf(rs.getString("unit_type")), rs.getBigDecimal("area_sqft"),
                        new Share(rs.getBigDecimal("share")), rs.getTimestamp("start_at").toInstant()),
                userId, size, (long) page * size);
    }

    @Override
    public long countPropertiesOf(UUID userId) {
        return jdbc.queryForObject("SELECT count(*)" + PROPERTIES, Long.class, userId);
    }

    @Override
    public long countOwnedUnits(UUID buildingId, UUID userId) {
        return jdbc.queryForObject("SELECT count(*) FROM ownership_period WHERE building_id = ? AND owner_user_id = ? "
                + "AND end_revision IS NULL", Long.class, buildingId, userId);
    }

    private OwnershipPeriod period(ResultSet rs, int row) throws SQLException {
        Timestamp end = rs.getTimestamp("end_at");
        long endValue = rs.getLong("end_revision");
        Long endRevision = rs.wasNull() ? null : endValue;
        return new OwnershipPeriod(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getObject("unit_id", UUID.class), rs.getObject("owner_user_id", UUID.class),
                new Share(rs.getBigDecimal("share")), rs.getTimestamp("start_at").toInstant(),
                rs.getLong("start_revision"), end == null ? null : end.toInstant(), endRevision,
                rs.getString("notes"), rs.getObject("created_by", UUID.class));
    }

    private OwnershipTransfer transfer(ResultSet rs, int row) throws SQLException {
        return new OwnershipTransfer(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                rs.getObject("unit_id", UUID.class), rs.getObject("source_owner_user_id", UUID.class),
                rs.getObject("recipient_user_id", UUID.class), new Share(rs.getBigDecimal("share")),
                rs.getDate("effective_date").toLocalDate(), rs.getTimestamp("effective_at").toInstant(),
                rs.getLong("revision"), rs.getObject("actor_user_id", UUID.class), rs.getString("reason"),
                rs.getString("reference"));
    }
}
