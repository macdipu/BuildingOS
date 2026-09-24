package com.buildingos.building.unit.infrastructure.persistence;

import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.model.UnitDetails;
import com.buildingos.building.unit.domain.model.UnitType;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUnitRepositoryAdapter implements UnitRepository {
    private static final String COLUMNS = "id, building_id, floor_id, number, unit_type, area_sqft, bedrooms, "
            + "default_maintenance_rate, notes, version, created_at, updated_at";
    private final JdbcTemplate jdbc;

    public JdbcUnitRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(Unit u) {
        var d = u.details();
        jdbc.update("INSERT INTO building_unit (id, building_id, floor_id, number, normalized_number, unit_type, "
                        + "area_sqft, bedrooms, default_maintenance_rate, notes, version, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                u.id(), u.buildingId(), d.floorId(), d.number(), d.normalizedNumber(), d.type().name(), d.areaSqft(),
                d.bedrooms(), d.defaultMaintenanceRate(), d.notes(), u.version(), Timestamp.from(u.createdAt()),
                Timestamp.from(u.updatedAt()));
    }

    @Override
    public void update(Unit u) {
        var d = u.details();
        int updated = jdbc.update("UPDATE building_unit SET floor_id = ?, number = ?, normalized_number = ?, "
                        + "unit_type = ?, area_sqft = ?, bedrooms = ?, default_maintenance_rate = ?, notes = ?, "
                        + "version = ?, updated_at = ? WHERE id = ? AND building_id = ? AND version = ?",
                d.floorId(), d.number(), d.normalizedNumber(), d.type().name(), d.areaSqft(), d.bedrooms(),
                d.defaultMaintenanceRate(), d.notes(), u.version(), Timestamp.from(u.updatedAt()), u.id(),
                u.buildingId(), u.version() - 1);
        if (updated != 1) {
            throw new IllegalStateException("Unit " + u.id() + " changed outside the building lock");
        }
    }

    @Override
    public Optional<Unit> findInBuilding(UUID buildingId, UUID unitId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_unit WHERE building_id = ? AND id = ?", this::map,
                buildingId, unitId).stream().findFirst();
    }

    @Override
    public List<Unit> findByBuilding(UUID buildingId, UUID floorId, UnitType type, int page, int size) {
        var args = filterArgs(buildingId, floorId, type);
        args.add(size);
        args.add((long) page * size);
        return jdbc.query("SELECT " + COLUMNS + " FROM building_unit" + filter(floorId, type)
                + " ORDER BY normalized_number, id LIMIT ? OFFSET ?", this::map, args.toArray());
    }

    @Override
    public long count(UUID buildingId, UUID floorId, UnitType type) {
        return jdbc.queryForObject("SELECT count(*) FROM building_unit" + filter(floorId, type), Long.class,
                filterArgs(buildingId, floorId, type).toArray());
    }

    @Override
    public long countByBuilding(UUID buildingId) {
        return count(buildingId, null, null);
    }

    @Override
    public boolean numberTaken(UUID buildingId, String normalizedNumber, UUID exceptUnitId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM building_unit "
                        + "WHERE building_id = ? AND normalized_number = ? AND id IS DISTINCT FROM CAST(? AS uuid))", Boolean.class,
                buildingId, normalizedNumber, exceptUnitId));
    }

    @Override
    public Set<String> takenNumbers(UUID buildingId, Collection<String> normalizedNumbers) {
        if (normalizedNumbers.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.query(connection -> {
            var statement = connection.prepareStatement("SELECT normalized_number FROM building_unit "
                    + "WHERE building_id = ? AND normalized_number = ANY (?)");
            statement.setObject(1, buildingId);
            statement.setArray(2, connection.createArrayOf("varchar", normalizedNumbers.toArray()));
            return statement;
        }, (rs, row) -> rs.getString(1)));
    }

    /** Shared with the batch adapter so both map rows identically. */
    static String columns() { return COLUMNS; }

    Unit mapRow(ResultSet rs, int row) throws SQLException { return map(rs, row); }

    private static String filter(UUID floorId, UnitType type) {
        return " WHERE building_id = ?" + (floorId == null ? "" : " AND floor_id = ?")
                + (type == null ? "" : " AND unit_type = ?");
    }

    private static List<Object> filterArgs(UUID buildingId, UUID floorId, UnitType type) {
        List<Object> args = new ArrayList<>();
        args.add(buildingId);
        if (floorId != null) {
            args.add(floorId);
        }
        if (type != null) {
            args.add(type.name());
        }
        return args;
    }

    private Unit map(ResultSet rs, int row) throws SQLException {
        int bedrooms = rs.getInt("bedrooms");
        Integer maybeBedrooms = rs.wasNull() ? null : bedrooms;
        return new Unit(rs.getObject("id", UUID.class), rs.getObject("building_id", UUID.class),
                new UnitDetails(rs.getString("number"), rs.getObject("floor_id", UUID.class),
                        UnitType.valueOf(rs.getString("unit_type")), rs.getBigDecimal("area_sqft"), maybeBedrooms,
                        rs.getBigDecimal("default_maintenance_rate"), rs.getString("notes")),
                rs.getLong("version"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
