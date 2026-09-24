package com.buildingos.building.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F4-T2: building selector/context, floors and units, activation prerequisite. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class UnitApiIntegrationTest {
    private static final String AUDIENCE = "building-platform";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID building;
    private UUID admin;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> AUDIENCE);
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void seedBuildingWithAdmin() {
        admin = UUID.randomUUID();
        building = seedBuilding("Rose Tower", "ONBOARDING");
        member(building, admin, "BUILDING_ADMIN");
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private UUID seedBuilding(String name, String status) {
        Instant now = Instant.now();
        UUID application = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(), Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) VALUES (?, ?, ?, 'RESIDENTIAL', "
                + "'Road 1', 'Mirpur', 'Dhaka', '01712345678', ?, 0, ?, ?)",
                id, application, name, status, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private void member(UUID parent, UUID user, String role) {
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE', now())", UUID.randomUUID(), parent, user, role);
    }

    private String token(UUID user, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of(roles));
    }

    private Reply send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private String base(UUID id) { return "/api/v1/buildings/" + id; }

    private Reply floor(UUID parent, String token, String label, int order) throws Exception {
        return send("POST", base(parent) + "/floors", token,
                "{\"label\":\"" + label + "\",\"kind\":\"REGULAR\",\"displayOrder\":" + order + "}");
    }

    private static String unitBody(String number, String floorId, String area) {
        return "{\"number\":\"" + number + "\",\"floorId\":\"" + floorId + "\",\"type\":\"FLAT\",\"areaSqft\":" + area
                + ",\"bedrooms\":3,\"defaultMaintenanceRate\":2500}";
    }

    private Reply unit(UUID parent, String token, String number, String floorId) throws Exception {
        return send("POST", base(parent) + "/units", token, unitBody(number, floorId, "1250.50"));
    }

    private int count(String table, UUID parent) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE building_id = ?", Integer.class, parent);
    }

    @Test
    void adminCreatesFloorsAndUnitsWithNormalizedUniqueNumbers() throws Exception {
        String adminToken = token(admin);
        var created = floor(building, adminToken, "1st Floor", 1);
        assertThat(created.status()).isEqualTo(201);
        String floorId = created.data().path("id").asString();
        assertThat(floor(building, adminToken, " 1ST FLOOR ", 2).code()).isEqualTo("FLOOR_LABEL_TAKEN");

        var first = unit(building, adminToken, "4a", floorId);
        assertThat(first.status()).isEqualTo(201);
        assertThat(first.data().path("number").asString()).isEqualTo("4a");
        assertThat(first.data().path("floorLabel").asString()).isEqualTo("1st Floor");
        assertThat(first.data().path("areaSqft").decimalValue()).isEqualByComparingTo("1250.50");
        assertThat(unit(building, adminToken, " 4A ", floorId).code()).isEqualTo("UNIT_NUMBER_TAKEN");

        UUID other = seedBuilding("Lily Court", "ACTIVE");
        member(other, admin, "BUILDING_ADMIN");
        String otherFloor = floor(other, adminToken, "1st Floor", 1).data().path("id").asString();
        assertThat(unit(other, adminToken, "4A", otherFloor).status()).isEqualTo(201);

        assertThat(send("POST", base(building) + "/units", adminToken, unitBody("5A", floorId, "10.123")).code())
                .isEqualTo("AREA_PRECISION");
        assertThat(send("POST", base(building) + "/units", adminToken, unitBody("5A", floorId, "0")).code())
                .isEqualTo("AREA_NOT_POSITIVE");
        assertThat(send("POST", base(building) + "/units", adminToken,
                unitBody("5A", floorId, "10").replace("FLAT", "PENTHOUSE")).status()).isEqualTo(400);
        assertThat(unit(building, adminToken, "5A", otherFloor).code()).isEqualTo("FLOOR_NOT_FOUND");
        assertThat(count("building_unit", building)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_audit WHERE building_id = ? "
                + "AND action IN ('FLOOR_CREATED', 'UNIT_CREATED')", Integer.class, building)).isEqualTo(2);

        var listed = send("GET", base(building) + "/units?type=FLAT", adminToken, null);
        assertThat(listed.body().path("meta").path("total").asInt()).isEqualTo(1);
        String unitId = first.data().path("id").asString();
        assertThat(send("GET", base(building) + "/units/" + unitId, adminToken, null).data().path("bedrooms").asInt())
                .isEqualTo(3);
        assertThat(send("GET", base(other) + "/units/" + unitId, adminToken, null).code())
                .isEqualTo("UNIT_NOT_FOUND");
    }

    @Test
    void updatesAreVersionedAndRenamesStayUnique() throws Exception {
        String adminToken = token(admin);
        String floorId = floor(building, adminToken, "Ground", 0).data().path("id").asString();
        String a = unit(building, adminToken, "G1", floorId).data().path("id").asString();
        unit(building, adminToken, "G2", floorId);

        String rename = unitBody("g2", floorId, "900").replace("}", ",\"expectedVersion\":0}");
        assertThat(send("PUT", base(building) + "/units/" + a, adminToken, rename).code())
                .isEqualTo("UNIT_NUMBER_TAKEN");
        String ok = unitBody("G1-A", floorId, "900").replace("}", ",\"expectedVersion\":0}");
        var updated = send("PUT", base(building) + "/units/" + a, adminToken, ok);
        assertThat(updated.data().path("number").asString()).isEqualTo("G1-A");
        assertThat(updated.data().path("version").asLong()).isEqualTo(1);
        assertThat(send("PUT", base(building) + "/units/" + a, adminToken, ok).code()).isEqualTo("STALE_VERSION");
        assertThat(send("PUT", base(building) + "/units/" + a, adminToken, unitBody("G1-B", floorId, "900")).status())
                .isEqualTo(400);

        var floorUpdate = send("PUT", base(building) + "/floors/" + floorId, adminToken,
                "{\"label\":\"Ground Floor\",\"kind\":\"GROUND\",\"displayOrder\":0,\"expectedVersion\":0}");
        assertThat(floorUpdate.data().path("kind").asString()).isEqualTo("GROUND");
        assertThat(send("GET", base(building) + "/units/" + a, adminToken, null).data().path("floorLabel").asString())
                .isEqualTo("Ground Floor");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_audit WHERE entity_id = ?::uuid "
                + "AND action = 'UNIT_UPDATED' AND before_data->>'number' = 'G1' AND after_data->>'number' = 'G1-A'",
                Integer.class, a)).isEqualTo(1);
    }

    @Test
    void concurrentCreatesOfOneNumberProduceOneUnit() throws Exception {
        String adminToken = token(admin);
        String floorId = floor(building, adminToken, "2nd", 2).data().path("id").asString();
        int callers = 6;
        var barrier = new CyclicBarrier(callers);
        var pool = Executors.newFixedThreadPool(callers);
        try {
            List<Future<Reply>> replies = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                String spelling = i % 2 == 0 ? "7b" : " 7B ";
                replies.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return unit(building, adminToken, spelling, floorId);
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (var reply : replies) {
                statuses.add(reply.get(30, TimeUnit.SECONDS).status());
            }
            assertThat(statuses).containsOnlyOnce(201).containsOnly(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(count("building_unit", building)).isEqualTo(1);
    }

    @Test
    void accessFollowsCurrentMembershipAndSuspendedBuildingsAreReadOnly() throws Exception {
        UUID owner = UUID.randomUUID();
        member(building, owner, "OWNER");
        assertThat(send("GET", base(building) + "/units", token(owner), null).body().path("meta").path("total")
                .asInt()).isZero();
        assertThat(send("GET", base(building) + "/floors", token(owner), null).status()).isEqualTo(403);
        assertThat(floor(building, token(owner), "Roof", 9).status()).isEqualTo(403);
        assertThat(send("GET", base(building) + "/units", token(UUID.randomUUID()), null).code())
                .isEqualTo("BUILDING_NOT_FOUND");

        String platform = token(UUID.randomUUID(), "PLATFORM_ADMIN");
        assertThat(floor(building, platform, "Roof", 9).code()).isEqualTo("INVALID_REQUEST");
        assertThat(send("POST", base(building) + "/floors", platform,
                "{\"label\":\"Roof\",\"kind\":\"ROOF\",\"displayOrder\":9,\"reason\":\"Support ticket 42\"}")
                .status()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT reason FROM building_audit WHERE building_id = ? "
                + "AND action = 'FLOOR_CREATED'", String.class, building)).isEqualTo("Support ticket 42");

        jdbc.update("UPDATE building SET status = 'SUSPENDED' WHERE id = ?", building);
        assertThat(floor(building, token(admin), "Basement", -1).code()).isEqualTo("BUILDING_READ_ONLY");
        assertThat(send("GET", base(building) + "/floors", token(admin), null).status()).isEqualTo(200);
    }

    @Test
    void selectorListsCurrentMembershipsAndContextReflectsRole() throws Exception {
        UUID owner = UUID.randomUUID();
        member(building, owner, "OWNER");
        UUID second = seedBuilding("Amber House", "ACTIVE");
        member(second, owner, "OWNER");
        member(second, owner, "BUILDING_ADMIN");
        UUID revoked = seedBuilding("Old Place", "ACTIVE");
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at, revoked_at, "
                + "revoked_by, revocation_reason) VALUES (?, ?, ?, 'OWNER', 'REVOKED', now(), now(), ?, 'Sold')",
                UUID.randomUUID(), revoked, owner, admin);

        var mine = send("GET", "/api/v1/me/buildings", token(owner), null);
        assertThat(mine.body().path("meta").path("total").asInt()).isEqualTo(2);
        assertThat(mine.data().get(0).path("name").asString()).isEqualTo("Amber House");
        assertThat(mine.data().get(0).path("roles").toString()).isEqualTo("[\"BUILDING_ADMIN\",\"OWNER\"]");
        assertThat(mine.data().get(1).path("name").asString()).isEqualTo("Rose Tower");
        assertThat(send("GET", "/api/v1/me/buildings", token(UUID.randomUUID()), null).data()).isEmpty();

        var ownerContext = send("GET", base(building), token(owner), null).data();
        assertThat(ownerContext.path("roles").toString()).isEqualTo("[\"OWNER\"]");
        assertThat(ownerContext.path("capabilities").path("manageUnits").asBoolean()).isFalse();
        var adminContext = send("GET", base(building), token(admin), null).data();
        assertThat(adminContext.path("capabilities").path("manageUnits").asBoolean()).isTrue();
        assertThat(send("GET", base(revoked), token(owner), null).status()).isEqualTo(404);
    }

    @Test
    void activationWaitsForAUnitAndSerializesWithUnitWrites() throws Exception {
        String platform = token(UUID.randomUUID(), "PLATFORM_ADMIN");
        String activate = "/api/v1/platform/buildings/" + building + "/activate";
        assertThat(send("POST", activate, platform, "{\"reason\":\"Ready?\"}").code()).isEqualTo("NO_BUILDING_UNIT");
        String floorId = floor(building, token(admin), "Ground", 0).data().path("id").asString();
        assertThat(send("POST", activate, platform, "{\"reason\":\"Ready?\"}").code()).isEqualTo("NO_BUILDING_UNIT");
        unit(building, token(admin), "G1", floorId);
        assertThat(send("POST", activate, platform, "{\"reason\":\"Units ready\"}").data().path("status").asString())
                .isEqualTo("ACTIVE");
    }
}
