package com.buildingos.building.building.infrastructure.persistence;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingStatus;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.buildingapplication.domain.model.BuildingType;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBuildingRepositoryAdapter implements BuildingRepository {
    private static final String COLUMNS = "id, application_id, name, building_type, address, area, district, "
            + "postal_code, latitude, longitude, contact_phone, status, version, created_at, updated_at";
    private final JdbcTemplate jdbc;

    public JdbcBuildingRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void insert(Building b) {
        var point = b.coordinates();
        jdbc.update("INSERT INTO building (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                b.id(), b.applicationId(), b.name(), b.type().name(), b.address(), b.area(), b.district(),
                b.postalCode(), point == null ? null : point.latitude(), point == null ? null : point.longitude(),
                b.contactPhone().value(), b.status().name(), b.version(), Timestamp.from(b.createdAt()),
                Timestamp.from(b.updatedAt()));
    }

    @Override
    public void update(Building b) {
        jdbc.update("UPDATE building SET status = ?, version = ?, updated_at = ? WHERE id = ?",
                b.status().name(), b.version(), Timestamp.from(b.updatedAt()), b.id());
    }

    @Override
    public Optional<Building> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building WHERE id = ?", this::map, id).stream().findFirst();
    }

    @Override
    public Optional<Building> findByIdForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building WHERE id = ? FOR UPDATE", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<Building> findByIdForShare(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building WHERE id = ? FOR SHARE", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<Building> findByApplication(UUID applicationId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building WHERE application_id = ?", this::map, applicationId)
                .stream().findFirst();
    }

    private Building map(ResultSet rs, int row) throws SQLException {
        BigDecimal latitude = rs.getBigDecimal("latitude");
        BigDecimal longitude = rs.getBigDecimal("longitude");
        return new Building(rs.getObject("id", UUID.class), rs.getObject("application_id", UUID.class),
                rs.getString("name"), BuildingType.valueOf(rs.getString("building_type")), rs.getString("address"),
                rs.getString("area"), rs.getString("district"), rs.getString("postal_code"),
                latitude == null ? null : new Coordinates(latitude.doubleValue(), longitude.doubleValue()),
                new ContactPhone(rs.getString("contact_phone")), BuildingStatus.valueOf(rs.getString("status")),
                rs.getInt("version"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
