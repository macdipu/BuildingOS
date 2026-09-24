package com.buildingos.building.unit.infrastructure.persistence;

import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.model.FloorDetails;
import com.buildingos.building.unit.domain.model.FloorKind;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFloorRepositoryAdapter implements FloorRepository {
    private static final String COLUMNS = "id, building_id, label, kind, display_order, version, created_at, updated_at";
    private static final String ORDER = " ORDER BY display_order, normalized_label, id";
    private final JdbcTemplate jdbc;

    public JdbcFloorRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(Floor f) {
        var d = f.details();
        jdbc.update("INSERT INTO building_floor (id, building_id, label, normalized_label, kind, display_order, "
                        + "version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                f.id(), f.buildingId(), d.label(), d.normalizedLabel(), d.kind().name(), d.displayOrder(), f.version(),
                Timestamp.from(f.createdAt()), Timestamp.from(f.updatedAt()));
    }

    @Override
    public void update(Floor f) {
        var d = f.details();
        int updated = jdbc.update("UPDATE building_floor SET label = ?, normalized_label = ?, kind = ?, "
                        + "display_order = ?, version = ?, updated_at = ? WHERE id = ? AND building_id = ? AND version = ?",
                d.label(), d.normalizedLabel(), d.kind().name(), d.displayOrder(), f.version(),
                Timestamp.from(f.updatedAt()), f.id(), f.buildingId(), f.version() - 1);
        if (updated != 1) {
            throw new IllegalStateException("Floor " + f.id() + " changed outside the building lock");
        }
    }

    @Override
    public Optional<Floor> findInBuilding(UUID buildingId, UUID floorId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_floor WHERE building_id = ? AND id = ?", this::map,
                buildingId, floorId).stream().findFirst();
    }

    @Override
    public List<Floor> findByBuilding(UUID buildingId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_floor WHERE building_id = ?" + ORDER, this::map,
                buildingId);
    }

    @Override
    public List<Floor> findByBuilding(UUID buildingId, int page, int size) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_floor WHERE building_id = ?" + ORDER
                + " LIMIT ? OFFSET ?", this::map, buildingId, size, (long) page * size);
    }

    @Override
    public long countByBuilding(UUID buildingId) {
        return jdbc.queryForObject("SELECT count(*) FROM building_floor WHERE building_id = ?", Long.class, buildingId);
    }

    @Override
    public boolean labelTaken(UUID buildingId, String normalizedLabel, UUID exceptFloorId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM building_floor "
                        + "WHERE building_id = ? AND normalized_label = ? AND id IS DISTINCT FROM CAST(? AS uuid))", Boolean.class,
                buildingId, normalizedLabel, exceptFloorId));
    }

    private Floor map(ResultSet rs, int row) throws SQLException {
        return new Floor(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                new FloorDetails(rs.getString("label"), FloorKind.valueOf(rs.getString("kind")),
                        rs.getInt("display_order")),
                rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
