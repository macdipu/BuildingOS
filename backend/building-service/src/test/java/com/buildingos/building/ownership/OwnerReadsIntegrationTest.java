package com.buildingos.building.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

/** F4-T4b: owner-filtered unit reads, current allocations, My Properties and owned-unit counts (UO-01/08). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OwnerReadsIntegrationTest {
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
    private UUID owner;
    private UUID coOwner;
    private final List<UUID> units = new ArrayList<>();
    private final String today = LocalDate.now(ZoneId.of("Asia/Dhaka")).toString();

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
    void seed() throws Exception {
        Instant now = Instant.now();
        UUID application = UUID.randomUUID();
        building = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(), Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) VALUES (?, ?, 'Rose Tower', 'RESIDENTIAL', "
                + "'Road 1', 'Mirpur', 'Dhaka', '01712345678', 'ACTIVE', 0, ?, ?)",
                building, application, Timestamp.from(now), Timestamp.from(now));
        admin = UUID.randomUUID();
        owner = UUID.randomUUID();
        coOwner = UUID.randomUUID();
        member(admin, "BUILDING_ADMIN");
        member(owner, "OWNER");
        member(coOwner, "OWNER");
        UUID floor = UUID.randomUUID();
        jdbc.update("INSERT INTO building_floor (id, building_id, label, normalized_label, kind, display_order, "
                + "created_at, updated_at) VALUES (?, ?, '1st', '1ST', 'REGULAR', 1, now(), now())", floor, building);
        units.clear();
        for (String number : List.of("1A", "1B", "1C", "1D")) {
            UUID unit = UUID.randomUUID();
            jdbc.update("INSERT INTO building_unit (id, building_id, floor_id, number, normalized_number, unit_type, "
                    + "area_sqft, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'FLAT', 900, now(), now())",
                    unit, building, floor, number, number);
            units.add(unit);
        }
        assign(units.get(0), owner, "50", 0);
        assign(units.get(0), coOwner, "50", 1);
        assign(units.get(1), owner, "100", 0);
        assign(units.get(2), owner, "25", 0);
        assign(units.get(3), coOwner, "100", 0);
    }

    private void member(UUID user, String role) {
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE', now())", UUID.randomUUID(), building, user, role);
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private Reply send(String method, String path, UUID user, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + fixtures.userToken(AUDIENCE, user, List.of()));
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private String unitPath(UUID unit) { return "/api/v1/buildings/" + building + "/units/" + unit; }

    private void assign(UUID unit, UUID user, String share, long version) throws Exception {
        var reply = send("POST", unitPath(unit) + "/ownerships", admin, "{\"ownerUserId\":\"" + user + "\",\"share\":"
                + share + ",\"effectiveDate\":\"" + today + "\",\"expectedVersion\":" + version + ",\"operationId\":\""
                + UUID.randomUUID() + "\"}");
        assertThat(reply.status()).isEqualTo(201);
    }

    @Test
    void ownersSeeOnlyUnitsTheyCurrentlyOwn() throws Exception {
        var listed = send("GET", "/api/v1/buildings/" + building + "/units", owner, null);
        assertThat(listed.body().path("meta").path("total").asInt()).isEqualTo(3);
        List<String> numbers = new ArrayList<>();
        listed.data().forEach(u -> numbers.add(u.path("number").asString()));
        assertThat(numbers).containsExactly("1A", "1B", "1C");
        assertThat(send("GET", unitPath(units.get(1)), owner, null).status()).isEqualTo(200);
        assertThat(send("GET", unitPath(units.get(3)), owner, null).code()).isEqualTo("UNIT_NOT_FOUND");
        assertThat(send("GET", unitPath(units.get(3)) + "/ownerships", owner, null).code()).isEqualTo("UNIT_NOT_FOUND");
        assertThat(send("GET", "/api/v1/buildings/" + building + "/units", admin, null).body().path("meta")
                .path("total").asInt()).isEqualTo(4);
    }

    @Test
    void currentAllocationsHideCoOwnersFromOwners() throws Exception {
        var mine = send("GET", unitPath(units.get(0)) + "/ownerships", owner, null).data();
        assertThat(mine.path("current")).hasSize(1);
        assertThat(mine.path("allocated").decimalValue()).isEqualByComparingTo("50");
        assertThat(mine.toString()).doesNotContain(coOwner.toString());
        var all = send("GET", unitPath(units.get(0)) + "/ownerships", admin, null).data();
        assertThat(all.path("current")).hasSize(2);
        assertThat(all.path("allocated").decimalValue()).isEqualByComparingTo("100");
        assertThat(all.path("revision").asLong()).isEqualTo(2);
    }

    @Test
    void myPropertiesAndOwnedCountsFollowCurrentMembership() throws Exception {
        var properties = send("GET", "/api/v1/me/properties", owner, null);
        assertThat(properties.body().path("meta").path("total").asInt()).isEqualTo(3);
        assertThat(properties.data().get(0).path("buildingName").asString()).isEqualTo("Rose Tower");
        assertThat(properties.data().get(0).path("unitNumber").asString()).isEqualTo("1A");
        assertThat(properties.data().get(0).path("share").decimalValue()).isEqualByComparingTo("50");
        assertThat(properties.data().get(2).path("share").decimalValue()).isEqualByComparingTo("25");
        assertThat(properties.toString()).doesNotContain(coOwner.toString());
        assertThat(send("GET", "/api/v1/me/buildings", owner, null).data().get(0).path("ownedUnitCount").asLong())
                .isEqualTo(3);
        assertThat(send("GET", "/api/v1/me/buildings", admin, null).data().get(0).path("ownedUnitCount").asLong())
                .isZero();

        jdbc.update("UPDATE building_membership SET status = 'REVOKED', revoked_at = now(), revoked_by = ?, "
                + "revocation_reason = 'Access removed', version = 1 WHERE user_id = ?", admin, owner);
        assertThat(send("GET", "/api/v1/me/properties", owner, null).data()).isEmpty();
        assertThat(send("GET", "/api/v1/me/buildings", owner, null).data()).isEmpty();
        assertThat(send("GET", unitPath(units.get(1)), owner, null).status()).isEqualTo(404);
    }
}
