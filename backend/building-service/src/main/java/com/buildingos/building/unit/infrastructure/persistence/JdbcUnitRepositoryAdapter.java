package com.buildingos.building.unit.infrastructure.persistence;

import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.model.UnitDetails;
import com.buildingos.building.unit.domain.model.UnitType;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import com.buildingos.building.unit.domain.model.UnitSearch;
import com.buildingos.building.unit.domain.model.UnitSort;
import java.util.Locale;
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
    public List<Unit> search(UUID buildingId, UnitSearch search, int page, int size) {
        var args = filterArgs(buildingId, search);
        args.add(size);
        args.add((long) page * size);
        return jdbc.query("SELECT " + COLUMNS + " FROM building_unit" + filter(search) + orderBy(search.sort())
                + " LIMIT ? OFFSET ?", this::map, args.toArray());
    }

    @Override
    public long count(UUID buildingId, UnitSearch search) {
        return jdbc.queryForObject("SELECT count(*) FROM building_unit" + filter(search), Long.class,
                filterArgs(buildingId, search).toArray());
    }

    @Override
    public boolean currentlyOwnedBy(UUID unitId, UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM ownership_period "
                + "WHERE unit_id = ? AND owner_user_id = ? AND end_revision IS NULL)", Boolean.class, unitId, userId));
    }

    @Override
    public long countByBuilding(UUID buildingId) {
        return count(buildingId, null, null, null);
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

    private static String filter(UnitSearch s) {
        return " WHERE building_id = ?" + (s.floorId() == null ? "" : " AND floor_id = ?")
                + (s.type() == null ? "" : " AND unit_type = ?")
                + (s.ownerUserId() == null ? "" : " AND EXISTS (SELECT 1 FROM ownership_period p WHERE p.unit_id = "
                        + "building_unit.id AND p.owner_user_id = ? AND p.end_revision IS NULL)")
                + (s.numberContains() == null ? "" : " AND normalized_number LIKE ? ESCAPE '\\'");
    }

    private static List<Object> filterArgs(UUID buildingId, UnitSearch s) {
        List<Object> args = new ArrayList<>();
        args.add(buildingId);
        if (s.floorId() != null) {
            args.add(s.floorId());
        }
        if (s.type() != null) {
            args.add(s.type().name());
        }
        if (s.ownerUserId() != null) {
            args.add(s.ownerUserId());
        }
        if (s.numberContains() != null) {
            args.add("%" + likeEscape(s.numberContains().toUpperCase(Locale.ROOT)) + "%");
        }
        return args;
    }

    private static String likeEscape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String orderBy(UnitSort sort) {
        String direction = sort.descending() ? " DESC" : " ASC";
        String key = switch (sort.field()) {
            case UNIT_NUMBER -> "";
            case FLOOR -> "(SELECT f.display_order FROM building_floor f WHERE f.id = building_unit.floor_id)"
                    + direction + ", ";
            case TYPE -> "unit_type" + direction + ", ";
        };
        String numberDirection = sort.field() == UnitSort.Field.UNIT_NUMBER ? direction : " ASC";
        return " ORDER BY " + key + "normalized_number" + numberDirection + ", id";
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
