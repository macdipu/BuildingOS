package com.buildingos.subscription.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.subscription.support.JwtFixtures;
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

/** F6-T5c (D-37): revenue audit rows read back through GET /internal/audit. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuditReadIntegrationTest {
    private static final String AUDIENCE = "subscription-platform";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("subscription_db").withUsername("subscription_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

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

    private void row(UUID actor, String action, String entityType, String entityId, String occurredAt) {
        jdbc.update("INSERT INTO audit_event (id, actor_user_id, action, entity_type, entity_id, before, after, "
                        + "occurred_at) VALUES (?, ?, ?, ?, ?, '{\"a\":1}'::jsonb, '{\"b\":2}'::jsonb, ?)",
                UUID.randomUUID(), actor, action, entityType, entityId, Timestamp.from(Instant.parse(occurredAt)));
    }

    private HttpResponse<String> get(String query, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/internal/audit" + query)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String superAdmin() throws Exception {
        return fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of("SUPER_ADMIN"));
    }

    private JsonNode read(String query) throws Exception {
        var response = get(query, superAdmin());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JSON.readTree(response.body()).path("data");
    }

    @Test
    void readsRowsNewestFirstFilteredAndLimitedWithoutPayloads() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        row(actor, "PLAN_CREATED", "SUBSCRIPTION_PLAN", "PREMIUM", "2026-01-01T10:00:00Z");
        row(actor, "FREE_TIER_UPDATED", "FREE_TIER", "DEFAULT", "2026-01-01T12:00:00Z");
        row(actor, "PLAN_UPDATED", "SUBSCRIPTION_PLAN", "PREMIUM", "2026-01-01T11:00:00Z");
        row(other, "PLAN_RETIRED", "SUBSCRIPTION_PLAN", "BASIC", "2026-01-01T11:30:00Z");

        JsonNode all = read("?actorUserId=" + actor);
        assertThat(all.findValuesAsString("occurredAt"))
                .containsExactly("2026-01-01T12:00:00Z", "2026-01-01T11:00:00Z", "2026-01-01T10:00:00Z");
        JsonNode item = all.get(0);
        assertThat(item.path("source").asString()).isEqualTo("subscription-service");
        assertThat(item.path("action").asString()).isEqualTo("FREE_TIER_UPDATED");
        assertThat(item.path("entityType").asString()).isEqualTo("FREE_TIER");
        assertThat(item.path("entityId").asString()).isEqualTo("DEFAULT");
        assertThat(item.path("actorUserId").asString()).isEqualTo(actor.toString());
        assertThat(item.path("buildingId").isNull()).isTrue();
        assertThat(item.path("reason").isNull()).isTrue();
        assertThat(item.has("before")).isFalse();
        assertThat(item.has("after")).isFalse();

        JsonNode window = read("?actorUserId=" + actor
                + "&since=2026-01-01T10:00:00Z&until=2026-01-01T12:00:00Z&entityType=SUBSCRIPTION_PLAN");
        assertThat(window.findValuesAsString("action")).containsExactly("PLAN_UPDATED", "PLAN_CREATED");

        JsonNode limited = read("?since=2026-01-01T00:00:00Z&until=2026-01-02T00:00:00Z&limit=2");
        assertThat(limited.findValuesAsString("occurredAt"))
                .containsExactly("2026-01-01T12:00:00Z", "2026-01-01T11:30:00Z");

        assertThat(read("?actorUserId=" + actor + "&entityType=BUILDING")).isEmpty();
        String token = superAdmin();
        assertThat(get("?limit=201", token).statusCode()).isEqualTo(400);
        assertThat(get("?limit=abc", token).statusCode()).isEqualTo(400);
        assertThat(get("?since=yesterday", token).statusCode()).isEqualTo(400);
        assertThat(get("?actorUserId=nobody", token).statusCode()).isEqualTo(400);
        assertThat(get("?limit=200", token).statusCode()).isEqualTo(200);
    }

    @Test
    void onlySuperAdminMayReadAndMissingTokenIsUnauthorized() throws Exception {
        for (String role : List.of("PLATFORM_ADMIN", "SUBSCRIPTION_ADMIN")) {
            String token = fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of(role));
            assertThat(get("", token).statusCode()).isEqualTo(403);
        }
        assertThat(get("", null).statusCode()).isEqualTo(401);
        assertThat(get("", "not-a-jwt").statusCode()).isEqualTo(401);
    }
}
