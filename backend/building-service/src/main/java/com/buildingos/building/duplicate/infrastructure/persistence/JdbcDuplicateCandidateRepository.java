package com.buildingos.building.duplicate.infrastructure.persistence;

import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import com.buildingos.building.duplicate.domain.model.CandidateKind;
import com.buildingos.building.duplicate.domain.model.DuplicateCandidate;
import com.buildingos.building.duplicate.domain.repository.DuplicateCandidateRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDuplicateCandidateRepository implements DuplicateCandidateRepository {
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320;
    private static final String NEAR = "(lower(district) = lower(?) OR contact_phone = ? "
            + "OR (latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?))";
    private static final int LIMIT = 50;
    private final JdbcTemplate jdbc;

    public JdbcDuplicateCandidateRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<DuplicateCandidate> findCandidates(UUID excludeApplicationId, String district, String contactPhone,
            Coordinates near, double radiusMeters) {
        Object[] box = box(near, radiusMeters);
        var result = new ArrayList<DuplicateCandidate>();
        result.addAll(jdbc.query("SELECT id, name AS reference, status, name, address, area, district, contact_phone, "
                        + "latitude, longitude FROM building WHERE " + NEAR + " ORDER BY created_at DESC LIMIT " + LIMIT,
                (rs, row) -> map(rs, CandidateKind.BUILDING), args(district, contactPhone, box)));
        result.addAll(jdbc.query("SELECT id, application_number AS reference, status, building_name AS name, address, "
                        + "area, district, contact_phone, latitude, longitude FROM building_application "
                        + "WHERE status NOT IN ('DRAFT', 'REJECTED', 'APPROVED') AND id <> ? AND " + NEAR
                        + " ORDER BY submitted_at DESC LIMIT " + LIMIT,
                (rs, row) -> map(rs, CandidateKind.APPLICATION),
                prepend(excludeApplicationId, args(district, contactPhone, box))));
        return result;
    }

    /** Degree bounding box around {@code near}; the matcher applies the exact radius. */
    private static Object[] box(Coordinates near, double radiusMeters) {
        if (near == null) {
            return new Object[] {null, null, null, null};
        }
        double dLat = radiusMeters / METERS_PER_DEGREE_LATITUDE;
        double dLon = dLat / Math.max(0.01, Math.cos(Math.toRadians(near.latitude())));
        return new Object[] {near.latitude() - dLat, near.latitude() + dLat, near.longitude() - dLon,
                near.longitude() + dLon};
    }

    private static Object[] args(String district, String contactPhone, Object[] box) {
        return new Object[] {district, contactPhone, box[0], box[1], box[2], box[3]};
    }

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] all = new Object[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    private static DuplicateCandidate map(ResultSet rs, CandidateKind kind) throws SQLException {
        BigDecimal latitude = rs.getBigDecimal("latitude");
        BigDecimal longitude = rs.getBigDecimal("longitude");
        return new DuplicateCandidate(kind, rs.getObject("id", UUID.class), rs.getString("reference"),
                rs.getString("status"), rs.getString("name"), rs.getString("address"), rs.getString("area"),
                rs.getString("district"), rs.getString("contact_phone"),
                latitude == null ? null : new Coordinates(latitude.doubleValue(), longitude.doubleValue()));
    }
}
