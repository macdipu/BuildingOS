package com.buildingos.building.buildingapplication.infrastructure.persistence;

import com.buildingos.building.buildingapplication.domain.model.ApplicantRelationship;
import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import com.buildingos.building.buildingapplication.domain.model.ApplicationNumber;
import com.buildingos.building.buildingapplication.domain.model.ApplicationSource;
import com.buildingos.building.buildingapplication.domain.model.ApplicationStatus;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.model.BuildingType;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import com.buildingos.building.buildingapplication.domain.model.ManagementType;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBuildingApplicationRepositoryAdapter implements BuildingApplicationRepository {
    private static final String COLUMNS = "id, application_number, applicant_user_id, building_name, building_type, "
            + "address, area, district, postal_code, total_floors, estimated_units, applicant_relationship, "
            + "relationship_note, contact_name, contact_phone, contact_email, management_type, latitude, longitude, "
            + "source, status, submitted_at, reviewed_at, reviewed_by, rejection_reason, info_request_message, "
            + "version, created_at, updated_at";
    private static final String SUBMITTED_FILTER = "status <> 'DRAFT' AND (?::varchar IS NULL OR status = ?)";
    private final JdbcTemplate jdbc;

    public JdbcBuildingApplicationRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public long nextNumberSequence() {
        return jdbc.queryForObject("SELECT nextval('building_application_number_seq')", Long.class);
    }

    @Override
    public void insert(BuildingApplication a) {
        var d = a.details();
        jdbc.update("INSERT INTO building_application (" + COLUMNS + ") VALUES "
                        + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                a.id(), a.number().value(), a.applicantUserId(), d.buildingName(), name(d.buildingType()), d.address(),
                d.area(), d.district(), d.postalCode(), d.totalFloors(), d.estimatedUnits(),
                name(d.applicantRelationship()), d.relationshipNote(), d.contactName(), phone(d.contactPhone()),
                d.contactEmail(), name(d.managementType()), latitude(d.coordinates()), longitude(d.coordinates()),
                a.source().name(), a.status().name(), ts(a.submittedAt()), ts(a.reviewedAt()), a.reviewedBy(),
                a.rejectionReason(), a.infoRequestMessage(), a.version(), ts(a.createdAt()), ts(a.updatedAt()));
    }

    @Override
    public void update(BuildingApplication a) {
        var d = a.details();
        jdbc.update("UPDATE building_application SET building_name = ?, building_type = ?, address = ?, area = ?, "
                        + "district = ?, postal_code = ?, total_floors = ?, estimated_units = ?, "
                        + "applicant_relationship = ?, relationship_note = ?, contact_name = ?, contact_phone = ?, "
                        + "contact_email = ?, management_type = ?, latitude = ?, longitude = ?, status = ?, "
                        + "submitted_at = ?, reviewed_at = ?, reviewed_by = ?, rejection_reason = ?, "
                        + "info_request_message = ?, version = ?, updated_at = ? WHERE id = ?",
                d.buildingName(), name(d.buildingType()), d.address(), d.area(), d.district(), d.postalCode(),
                d.totalFloors(), d.estimatedUnits(), name(d.applicantRelationship()), d.relationshipNote(),
                d.contactName(), phone(d.contactPhone()), d.contactEmail(), name(d.managementType()),
                latitude(d.coordinates()), longitude(d.coordinates()), a.status().name(), ts(a.submittedAt()),
                ts(a.reviewedAt()), a.reviewedBy(), a.rejectionReason(), a.infoRequestMessage(), a.version(),
                ts(a.updatedAt()), a.id());
    }

    @Override
    public Optional<BuildingApplication> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_application WHERE id = ?", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<BuildingApplication> findByIdForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_application WHERE id = ? FOR UPDATE", this::map, id)
                .stream().findFirst();
    }

    @Override
    public List<BuildingApplication> findByApplicant(UUID applicantUserId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM building_application WHERE applicant_user_id = ? "
                + "ORDER BY created_at DESC, id", this::map, applicantUserId);
    }

    @Override
    public List<BuildingApplication> findPage(ApplicationStatus status, int offset, int limit) {
        String value = name(status);
        return jdbc.query("SELECT " + COLUMNS + " FROM building_application WHERE " + SUBMITTED_FILTER
                + " ORDER BY submitted_at, id LIMIT ? OFFSET ?", this::map, value, value, limit, offset);
    }

    @Override
    public long count(ApplicationStatus status) {
        String value = name(status);
        return jdbc.queryForObject("SELECT count(*) FROM building_application WHERE " + SUBMITTED_FILTER,
                Long.class, value, value);
    }

    private BuildingApplication map(ResultSet rs, int row) throws SQLException {
        BigDecimal latitude = rs.getBigDecimal("latitude");
        BigDecimal longitude = rs.getBigDecimal("longitude");
        var details = new ApplicationDetails(rs.getString("building_name"),
                value(rs.getString("building_type"), BuildingType::valueOf), rs.getString("address"),
                rs.getString("area"), rs.getString("district"), rs.getString("postal_code"),
                rs.getObject("total_floors", Integer.class), rs.getObject("estimated_units", Integer.class),
                value(rs.getString("applicant_relationship"), ApplicantRelationship::valueOf),
                rs.getString("relationship_note"), rs.getString("contact_name"),
                value(rs.getString("contact_phone"), ContactPhone::new), rs.getString("contact_email"),
                value(rs.getString("management_type"), ManagementType::valueOf),
                latitude == null ? null : new Coordinates(latitude.doubleValue(), longitude.doubleValue()));
        return new BuildingApplication(rs.getObject("id", UUID.class),
                new ApplicationNumber(rs.getString("application_number")),
                rs.getObject("applicant_user_id", UUID.class), details,
                ApplicationSource.valueOf(rs.getString("source")), ApplicationStatus.valueOf(rs.getString("status")),
                instant(rs.getTimestamp("submitted_at")), instant(rs.getTimestamp("reviewed_at")),
                rs.getObject("reviewed_by", UUID.class), rs.getString("rejection_reason"),
                rs.getString("info_request_message"), rs.getInt("version"), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at")));
    }

    private static <T> T value(String raw, Function<String, T> parse) { return raw == null ? null : parse.apply(raw); }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }

    private static String phone(ContactPhone phone) { return phone == null ? null : phone.value(); }

    private static Double latitude(Coordinates c) { return c == null ? null : c.latitude(); }

    private static Double longitude(Coordinates c) { return c == null ? null : c.longitude(); }

    private static Timestamp ts(Instant instant) { return instant == null ? null : Timestamp.from(instant); }

    private static Instant instant(Timestamp ts) { return ts == null ? null : ts.toInstant(); }
}
