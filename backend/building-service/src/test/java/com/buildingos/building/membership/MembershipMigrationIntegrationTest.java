package com.buildingos.building.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.building.domain.model.MembershipStatus;
import com.buildingos.building.building.infrastructure.persistence.JdbcBuildingMembershipRepositoryAdapter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** Real V4-to-V5 upgrade and database integrity checks; no persistent developer database. */
@Testcontainers
class MembershipMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    private static final Instant CREATED = Instant.parse("2026-09-20T06:00:00Z");
    private JdbcTemplate jdbc;
    private Flyway latest;
    private UUID building;
    private UUID admin;
    private UUID membership;

    @BeforeEach
    void upgradeFromF2() {
        String schema = "membership_" + UUID.randomUUID().toString().replace("-", "");
        String url = POSTGRES.getJdbcUrl();
        var source = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
                POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(source).schemas(schema).target("4").load().migrate();
        jdbc = new JdbcTemplate(source);
        building = seedBuilding();
        admin = UUID.randomUUID();
        membership = UUID.randomUUID();
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, 'BUILDING_ADMIN', 'ACTIVE', ?)",
                membership, building, admin, Timestamp.from(CREATED));
        latest = Flyway.configure().dataSource(source).schemas(schema).load();
        latest.migrate();
    }

    private UUID seedBuilding() {
        UUID application = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(),
                Timestamp.from(CREATED), Timestamp.from(CREATED));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) "
                + "VALUES (?, ?, 'Test', 'RESIDENTIAL', 'Road 1', 'Mirpur', 'Dhaka', '01712345678', "
                + "'ONBOARDING', 0, ?, ?)", id, application, Timestamp.from(CREATED), Timestamp.from(CREATED));
        return id;
    }

    private UUID invite(UUID parent, String phone) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO building_invitation (id, building_id, phone, role, status, created_by, reason, "
                + "created_at, expires_at) VALUES (?, ?, ?, 'OWNER', 'PENDING', ?, 'Owner invited', ?, ?)",
                id, parent, phone, admin, Timestamp.from(CREATED), Timestamp.from(CREATED.plusSeconds(604800)));
        return id;
    }

    private void member(UUID parent, UUID user) {
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, 'OWNER', 'ACTIVE', ?)", UUID.randomUUID(), parent, user, Timestamp.from(CREATED));
    }

    @Test
    void preservesInitialAdminAndBackfillsOriginalTime() {
        var adapter = new JdbcBuildingMembershipRepositoryAdapter(jdbc);
        var members = adapter.findByBuilding(building);
        assertThat(members).hasSize(1);
        assertThat(members.get(0).id()).isEqualTo(membership);
        assertThat(members.get(0).userId()).isEqualTo(admin);
        assertThat(members.get(0).createdAt()).isEqualTo(CREATED);
        assertThat(adapter.countActiveAdmins(building)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM building_membership WHERE id = ?",
                Timestamp.class, membership).toInstant()).isEqualTo(CREATED);
        assertThat(jdbc.queryForObject("SELECT version FROM building_membership WHERE id = ?",
                Long.class, membership)).isZero();
        assertThat(latest.migrate().migrationsExecuted).isZero();
    }

    @Test
    void ownerAndRevokedRowsRemainReadableWithoutCountingAsAdmins() {
        UUID user = UUID.randomUUID();
        member(building, user);
        jdbc.update("UPDATE building_membership SET status = 'REVOKED', revoked_at = ?, revoked_by = ?, "
                + "revocation_reason = 'Access removed', updated_at = ?, version = 1 WHERE user_id = ?",
                Timestamp.from(CREATED.plusSeconds(1)), admin, Timestamp.from(CREATED.plusSeconds(1)), user);
        var adapter = new JdbcBuildingMembershipRepositoryAdapter(jdbc);
        assertThat(adapter.findByBuilding(building))
                .anySatisfy(m -> {
                    assertThat(m.role()).isEqualTo(BuildingRole.OWNER);
                    assertThat(m.status()).isEqualTo(MembershipStatus.REVOKED);
                });
        assertThat(adapter.countActiveAdmins(building)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE building_membership SET status = 'REVOKED' WHERE id = ?",
                membership)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onePendingInvitePerBuildingPhoneAndTerminalHistoryAllowsReplacement() {
        UUID first = invite(building, "01712345678");
        assertThatThrownBy(() -> invite(building, "01712345678"))
                .isInstanceOf(DataIntegrityViolationException.class);
        invite(seedBuilding(), "01712345678");
        jdbc.update("UPDATE building_invitation SET status = 'EXPIRED', version = 1 WHERE id = ?", first);
        invite(building, "01712345678");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_invitation WHERE building_id = ?",
                Integer.class, building)).isEqualTo(2);
    }

    @Test
    void claimRequiresAnOwnerMembershipInTheSameBuildingAndValidClaimTime() {
        UUID invitation = invite(building, "01812345678");
        UUID owner = UUID.randomUUID();
        member(seedBuilding(), owner);
        assertThatThrownBy(() -> claim(invitation, owner, CREATED.plusSeconds(10)))
                .isInstanceOf(DataIntegrityViolationException.class);
        member(building, owner);
        assertThatThrownBy(() -> claim(invitation, owner, CREATED.plusSeconds(604800)))
                .isInstanceOf(DataIntegrityViolationException.class);
        claim(invitation, owner, CREATED.plusSeconds(10));
        assertThat(jdbc.queryForObject("SELECT claimed_user_id FROM building_invitation WHERE id = ?",
                UUID.class, invitation)).isEqualTo(owner);
    }

    private void claim(UUID invitation, UUID owner, Instant when) {
        jdbc.update("UPDATE building_invitation SET status = 'CLAIMED', claimed_user_id = ?, claimed_at = ?, "
                + "version = 1 WHERE id = ?", owner, Timestamp.from(when), invitation);
    }

    @Test
    void rejectsInvalidPhoneOrStateMetadataAndUnknownBuildings() {
        assertThatThrownBy(() -> invite(building, "+8801712345678"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> invite(UUID.randomUUID(), "01712345678"))
                .isInstanceOf(DataIntegrityViolationException.class);
        UUID invitation = invite(building, "01912345678");
        assertThatThrownBy(() -> jdbc.update("UPDATE building_invitation SET status = 'REVOKED' WHERE id = ?",
                invitation)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE building_invitation SET role = 'BUILDING_ADMIN' WHERE id = ?",
                invitation)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void operationIdsAreUniquePerActorAndActionAndAuditRequiresObjectPayloads() {
        UUID operation = UUID.randomUUID();
        String sql = "INSERT INTO building_operation (actor_user_id, action, operation_id, building_id, "
                + "request_fingerprint, result_entity_id, result_version, created_at) VALUES (?, ?, ?, ?, ?, ?, 0, ?)";
        jdbc.update(sql, admin, "CLAIM_INVITATION", operation, building, "a".repeat(64), membership, Timestamp.from(CREATED));
        assertThatThrownBy(() -> jdbc.update(sql, admin, "CLAIM_INVITATION", operation, building,
                "b".repeat(64), membership, Timestamp.from(CREATED))).isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update(sql, admin, "OTHER_ACTION", operation, building, "a".repeat(64), membership, Timestamp.from(CREATED));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO building_audit "
                + "(id, building_id, actor_user_id, action, entity_type, entity_id, reason, after_data, occurred_at) "
                + "VALUES (?, ?, ?, 'INVITE', 'INVITATION', ?, 'Requested', '[]', ?)",
                UUID.randomUUID(), building, admin, UUID.randomUUID(), Timestamp.from(CREATED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
