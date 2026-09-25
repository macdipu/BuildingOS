package com.buildingos.backoffice.auditlog;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.backoffice.support.AuditStub;
import com.buildingos.backoffice.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
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

/** F6-T5d (BOC-05, D-37): audit aggregation over stubbed auth/building/subscription plus own lifecycle_transition. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuditLogApiIntegrationTest {
    private static final String AUDIENCE = "backoffice-platform";
    private static final String PATH = "/api/v1/platform/backoffice/audit";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;
    private static AuditStub auth;
    private static AuditStub building;
    private static AuditStub subscription;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("back_office_db").withUsername("back_office_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
        auth = new AuditStub("auth-service");
        building = new AuditStub("building-service");
        subscription = new AuditStub("subscription-service");
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
        auth.close();
        building.close();
        subscription.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> AUDIENCE);
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("buildingos.services.auth-url", auth::url);
        registry.add("buildingos.services.building-url", building::url);
        registry.add("buildingos.services.subscription-url", subscription::url);
        registry.add("buildingos.audit.timeout", () -> "PT0.5S");
    }

    @BeforeEach
    void reset() {
        auth.reset();
        building.reset();
        subscription.reset();
        jdbc.update("DELETE FROM lifecycle_transition");
    }

    private void ownTransition(String id, String at, String from, String to, UUID actor) {
        jdbc.update("INSERT INTO lifecycle_transition (id, entity_type, entity_id, from_status, to_status, "
                        + "actor_user_id, reason, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.fromString(id), "SUPPORT_SESSION", UUID.randomUUID(), from, to, actor, "ticket 42",
                Timestamp.from(Instant.parse(at)));
    }

    private static String superAdmin() throws Exception {
        return fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of("SUPER_ADMIN"));
    }

    private HttpResponse<String> get(String token, String query) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + PATH + query)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode okData(String query) throws Exception {
        String token = superAdmin();
        var response = get(token, query);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JSON.readTree(response.body()).path("data");
    }

    private static List<String> ids(JsonNode data) {
        List<String> ids = new ArrayList<>();
        data.path("items").forEach(item -> ids.add(item.path("id").asString()));
        return ids;
    }

    private static List<String> unavailable(JsonNode data) {
        List<String> names = new ArrayList<>();
        data.path("unavailableSources").forEach(name -> names.add(name.asString()));
        return names;
    }

    private void seedAll() {
        auth.events.add(auth.event("00000000-0000-0000-0000-00000000000a", "2026-09-25T10:00:05Z",
                "PLATFORM_ROLE_GRANTED"));
        auth.events.add(auth.event("00000000-0000-0000-0000-00000000000b", "2026-09-25T10:00:01Z",
                "PLATFORM_ROLE_REVOKED"));
        building.events.add(building.event("00000000-0000-0000-0000-00000000000c", "2026-09-25T10:00:04Z",
                "NONE→SUBMITTED"));
        subscription.events.add(subscription.event("00000000-0000-0000-0000-00000000000d", "2026-09-25T10:00:02Z",
                "PLAN_CREATED"));
        ownTransition("00000000-0000-0000-0000-00000000000e", "2026-09-25T10:00:03Z", null, "ACTIVE", null);
    }

    @Test
    void mergesAllSourcesNewestFirstAndRelaysToken() throws Exception {
        seedAll();
        String token = superAdmin();
        var response = get(token, "?entityType=X&since=2026-09-25T00:00:00Z&limit=10");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        var data = JSON.readTree(response.body()).path("data");
        // Own rows are filtered by entityType too: X is not a back-office type, so none.
        assertThat(ids(data)).containsExactly("00000000-0000-0000-0000-00000000000a",
                "00000000-0000-0000-0000-00000000000c", "00000000-0000-0000-0000-00000000000d",
                "00000000-0000-0000-0000-00000000000b");
        assertThat(unavailable(data)).isEmpty();
        assertThat(data.has("nextUntil")).isTrue();
        assertThat(data.path("nextUntil").isNull()).isTrue();
        for (AuditStub stub : List.of(auth, building, subscription)) {
            assertThat(stub.authorizations).containsExactly("Bearer " + token);
            assertThat(stub.queries.get(0)).contains("limit=10").contains("entityType=X")
                    .contains("since=2026-09-25T00:00:00Z");
        }

        var all = okData("?limit=3");
        assertThat(ids(all)).containsExactly("00000000-0000-0000-0000-00000000000a",
                "00000000-0000-0000-0000-00000000000c", "00000000-0000-0000-0000-00000000000e");
        assertThat(all.path("nextUntil").asString()).isEqualTo("2026-09-25T10:00:03Z");
        JsonNode own = all.path("items").get(2);
        assertThat(own.path("source").asString()).isEqualTo("back-office-service");
        assertThat(own.path("action").asString()).isEqualTo("NONE→ACTIVE");
        assertThat(own.path("entityType").asString()).isEqualTo("SUPPORT_SESSION");
        assertThat(own.path("reason").asString()).isEqualTo("ticket 42");
        assertThat(own.path("actorUserId").isNull()).isTrue();
    }

    @Test
    void failingSourceIsListedUnavailableOthersStillReturned() throws Exception {
        seedAll();
        building.status = 503;
        subscription.delayMs = 3000;
        var data = okData("");
        assertThat(unavailable(data)).containsExactly("building-service", "subscription-service");
        assertThat(ids(data)).containsExactly("00000000-0000-0000-0000-00000000000a",
                "00000000-0000-0000-0000-00000000000e", "00000000-0000-0000-0000-00000000000b");
    }

    @Test
    void sourceFilterQueriesOnlyThatSource() throws Exception {
        seedAll();
        var data = okData("?source=back-office-service");
        assertThat(ids(data)).containsExactly("00000000-0000-0000-0000-00000000000e");
        assertThat(auth.queries).isEmpty();
        assertThat(building.queries).isEmpty();
        assertThat(subscription.queries).isEmpty();

        var onlyBuilding = okData("?source=building-service");
        assertThat(ids(onlyBuilding)).containsExactly("00000000-0000-0000-0000-00000000000c");
        assertThat(auth.queries).isEmpty();
    }

    @Test
    void invalidParametersAreBadRequest() throws Exception {
        String token = superAdmin();
        for (String query : List.of("?limit=0", "?limit=201", "?limit=abc", "?since=yesterday", "?until=x",
                "?actorUserId=not-a-uuid", "?source=payment-service")) {
            var response = get(token, query);
            assertThat(response.statusCode()).as(query).isEqualTo(400);
            assertThat(JSON.readTree(response.body()).path("code").asString()).isEqualTo("INVALID_REQUEST");
        }
    }

    @Test
    void nonSuperAdminIsForbidden() throws Exception {
        for (String role : List.of("PLATFORM_ADMIN", "SUPPORT_AGENT")) {
            var response = get(fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of(role)), "");
            assertThat(response.statusCode()).as(role).isEqualTo(403);
            assertThat(JSON.readTree(response.body()).path("code").asString()).isEqualTo("ACCESS_DENIED");
        }
        assertThat(auth.queries).isEmpty();
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        assertThat(get(null, "").statusCode()).isEqualTo(401);
    }
}
