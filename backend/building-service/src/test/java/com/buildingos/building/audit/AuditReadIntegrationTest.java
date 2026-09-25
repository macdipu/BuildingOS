package com.buildingos.building.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
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

/** F6-T5c (D-37): lifecycle transitions and building audit rows read back through GET /internal/audit. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuditReadIntegrationTest {
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

    private UUID actor;
    private UUID building;
    private UUID application;

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
    void seed() {
        Instant now = Instant.now();
        actor = UUID.randomUUID();
        application = UUID.randomUUID();
        building = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(), Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) VALUES (?, ?, 'Rose Tower', 'RESIDENTIAL', "
                + "'Road 1', 'Mirpur', 'Dhaka', '01712345678', 'ACTIVE', 0, ?, ?)",
                building, application, Timestamp.from(now), Timestamp.from(now));
    }

    private void transition(String type, UUID entity, String from, String to, UUID by, String reason, String at) {
        jdbc.update("INSERT INTO lifecycle_transition (id, entity_type, entity_id, from_status, to_status, "
                        + "actor_user_id, reason, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), type, entity, from, to, by, reason, Timestamp.from(Instant.parse(at)));
    }

    private void audit(String action, String type, UUID by, String at) {
        jdbc.update("INSERT INTO building_audit (id, building_id, actor_user_id, action, entity_type, entity_id, "
                        + "reason, before_data, after_data, occurred_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'Audited', '{\"a\":\"1\"}'::jsonb, '{\"b\":\"2\"}'::jsonb, ?)",
                UUID.randomUUID(), building, by, action, type, UUID.randomUUID(), Timestamp.from(Instant.parse(at)));
    }

    private HttpResponse<String> get(String query, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/internal/audit" + query)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode read(String query) throws Exception {
        var response = get(query, superAdmin());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JSON.readTree(response.body()).path("data");
    }

    private String superAdmin() throws Exception {
        return fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of("SUPER_ADMIN"));
    }

    @Test
    void mergesTransitionsAndAuditRowsNewestFirstWithoutPayloads() throws Exception {
        transition("BUILDING_APPLICATION", application, null, "DRAFT", actor, null, "2026-01-01T09:00:00Z");
        audit("MEMBER_INVITED", "BUILDING_INVITATION", actor, "2026-01-01T10:00:00Z");
        transition("BUILDING", building, "ONBOARDING", "ACTIVE", actor, "Ready", "2026-01-01T11:00:00Z");

        JsonNode items = read("?actorUserId=" + actor);
        assertThat(items.findValuesAsString("occurredAt"))
                .containsExactly("2026-01-01T11:00:00Z", "2026-01-01T10:00:00Z", "2026-01-01T09:00:00Z");
        assertThat(items.findValuesAsString("action"))
                .containsExactly("ONBOARDING→ACTIVE", "MEMBER_INVITED", "NONE→DRAFT");
        assertThat(items.findValuesAsString("source")).containsOnly("building-service");

        JsonNode activation = items.get(0);
        assertThat(activation.path("entityType").asString()).isEqualTo("BUILDING");
        assertThat(activation.path("entityId").asString()).isEqualTo(building.toString());
        assertThat(activation.path("buildingId").asString()).isEqualTo(building.toString());
        assertThat(activation.path("reason").asString()).isEqualTo("Ready");
        assertThat(activation.path("actorUserId").asString()).isEqualTo(actor.toString());

        JsonNode invited = items.get(1);
        assertThat(invited.path("buildingId").asString()).isEqualTo(building.toString());
        assertThat(invited.path("reason").asString()).isEqualTo("Audited");
        assertThat(invited.has("before")).isFalse();
        assertThat(invited.has("after")).isFalse();
        assertThat(invited.has("beforeData")).isFalse();

        assertThat(items.get(2).path("buildingId").isNull()).isTrue();
        assertThat(items.get(2).path("reason").isNull()).isTrue();
    }

    @Test
    void filtersByWindowAndEntityTypeAndLimitsAfterMerge() throws Exception {
        transition("BUILDING", building, "ONBOARDING", "ACTIVE", actor, "Ready", "2026-02-01T12:00:00Z");
        audit("MEMBER_INVITED", "BUILDING_INVITATION", actor, "2026-02-01T11:30:00Z");
        transition("BUILDING", building, "ACTIVE", "SUSPENDED", actor, "Unpaid", "2026-02-01T11:00:00Z");
        audit("UNIT_CREATED", "BUILDING", actor, "2026-02-01T10:00:00Z");

        JsonNode window = read("?actorUserId=" + actor
                + "&since=2026-02-01T11:00:00Z&until=2026-02-01T12:00:00Z");
        assertThat(window.findValuesAsString("occurredAt"))
                .containsExactly("2026-02-01T11:30:00Z", "2026-02-01T11:00:00Z");

        JsonNode buildings = read("?actorUserId=" + actor + "&entityType=BUILDING");
        assertThat(buildings.findValuesAsString("action"))
                .containsExactly("ONBOARDING→ACTIVE", "ACTIVE→SUSPENDED", "UNIT_CREATED");

        JsonNode limited = read("?actorUserId=" + actor + "&limit=2");
        assertThat(limited.findValuesAsString("occurredAt"))
                .containsExactly("2026-02-01T12:00:00Z", "2026-02-01T11:30:00Z");

        assertThat(read("?actorUserId=" + actor + "&entityType=PLATFORM_ROLE")).isEmpty();
        assertThat(read("?actorUserId=" + actor + "&since=2026-02-02T00:00:00Z&until=2026-02-01T00:00:00Z")).isEmpty();
        String token = superAdmin();
        assertThat(get("?limit=201", token).statusCode()).isEqualTo(400);
        assertThat(get("?limit=0", token).statusCode()).isEqualTo(400);
        assertThat(get("?limit=abc", token).statusCode()).isEqualTo(400);
        assertThat(get("?since=yesterday", token).statusCode()).isEqualTo(400);
        assertThat(get("?actorUserId=nobody", token).statusCode()).isEqualTo(400);
        assertThat(get("?limit=200", token).statusCode()).isEqualTo(200);
    }

    @Test
    void onlySuperAdminMayReadAndMissingTokenIsUnauthorized() throws Exception {
        for (String role : List.of("PLATFORM_ADMIN", "SUPPORT_AGENT")) {
            String token = fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of(role));
            assertThat(get("", token).statusCode()).isEqualTo(403);
        }
        assertThat(get("", null).statusCode()).isEqualTo(401);
        assertThat(get("", "not-a-jwt").statusCode()).isEqualTo(401);
    }
}
