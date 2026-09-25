package com.buildingos.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.auth.support.JwtFixtures;
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

/** F6-T5b (D-37): platform-role grant/revoke are audited and read back through GET /internal/audit. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuditReadIntegrationTest {
    private static final String AUDIENCE = "auth-platform";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("auth_db").withUsername("auth_app").withPassword("test-only-password");

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

    private UUID user(String phone) {
        return jdbc.queryForObject("INSERT INTO app_user (phone) VALUES (?) RETURNING id", UUID.class, phone);
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode audit(String token, String query) throws Exception {
        var response = send("GET", "/internal/audit" + query, token, null);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JSON.readTree(response.body()).path("data");
    }

    private void row(UUID actor, UUID target, String role, String action, String occurredAt) {
        jdbc.update("""
                INSERT INTO platform_role_audit (id, actor_user_id, target_user_id, role, action, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), actor, target, role, action, Timestamp.from(Instant.parse(occurredAt)));
    }

    @Test
    void grantAndRevokeWriteAuditRowsReadableBySuperAdmin() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID target = user("01755700011");
        String superAdmin = fixtures.userToken(AUDIENCE, actor, List.of("SUPER_ADMIN"));

        var granted = send("POST", "/internal/users/" + target + "/platform-roles", superAdmin,
                "{\"role\":\"SUPPORT_AGENT\"}");
        assertThat(granted.statusCode()).isEqualTo(200);
        send("POST", "/internal/users/" + target + "/platform-roles", superAdmin, "{\"role\":\"SUPPORT_AGENT\"}");
        assertThat(send("DELETE", "/internal/users/" + target + "/platform-roles/SUPPORT_AGENT", superAdmin, null)
                .statusCode()).isEqualTo(204);

        assertThat(jdbc.queryForList(
                "SELECT action FROM platform_role_audit WHERE target_user_id = ? ORDER BY occurred_at", String.class,
                target)).containsExactly("GRANT", "REVOKE");

        JsonNode items = audit(superAdmin, "?actorUserId=" + actor);
        assertThat(items).hasSize(2);
        assertThat(items.findValuesAsString("action"))
                .containsExactlyInAnyOrder("PLATFORM_ROLE_GRANTED", "PLATFORM_ROLE_REVOKED");
        JsonNode item = items.get(0);
        assertThat(item.path("source").asString()).isEqualTo("auth-service");
        assertThat(item.path("entityType").asString()).isEqualTo("PLATFORM_ROLE");
        assertThat(item.path("entityId").asString()).isEqualTo(target.toString());
        assertThat(item.path("actorUserId").asString()).isEqualTo(actor.toString());
        assertThat(item.path("role").asString()).isEqualTo("SUPPORT_AGENT");
        assertThat(item.path("buildingId").isNull()).isTrue();
        assertThat(item.path("reason").isNull()).isTrue();
        assertThat(item.has("before")).isFalse();
        assertThat(item.has("after")).isFalse();
    }

    @Test
    void listFiltersOrdersNewestFirstAndLimits() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        row(actor, target, "SUPPORT_AGENT", "GRANT", "2026-01-01T10:00:00Z");
        row(actor, target, "SUPPORT_AGENT", "REVOKE", "2026-01-01T12:00:00Z");
        row(actor, target, "PLATFORM_ADMIN", "GRANT", "2026-01-01T11:00:00Z");
        row(other, target, "ONBOARDING_AGENT", "GRANT", "2026-01-01T11:30:00Z");
        String superAdmin = fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of("SUPER_ADMIN"));

        JsonNode all = audit(superAdmin, "?actorUserId=" + actor);
        assertThat(all.findValuesAsString("occurredAt"))
                .containsExactly("2026-01-01T12:00:00Z", "2026-01-01T11:00:00Z", "2026-01-01T10:00:00Z");

        JsonNode window = audit(superAdmin, "?actorUserId=" + actor
                + "&since=2026-01-01T11:00:00Z&until=2026-01-01T12:00:00Z&entityType=PLATFORM_ROLE");
        assertThat(window).hasSize(1);
        assertThat(window.get(0).path("role").asString()).isEqualTo("PLATFORM_ADMIN");

        JsonNode limited = audit(superAdmin, "?since=2026-01-01T00:00:00Z&until=2026-01-02T00:00:00Z&limit=2");
        assertThat(limited.findValuesAsString("occurredAt"))
                .containsExactly("2026-01-01T12:00:00Z", "2026-01-01T11:30:00Z");

        assertThat(audit(superAdmin, "?entityType=BUILDING")).isEmpty();
        assertThat(send("GET", "/internal/audit?limit=201", superAdmin, null).statusCode()).isEqualTo(400);
        assertThat(send("GET", "/internal/audit?since=yesterday", superAdmin, null).statusCode()).isEqualTo(400);
        assertThat(send("GET", "/internal/audit?limit=200", superAdmin, null).statusCode()).isEqualTo(200);
    }

    @Test
    void onlySuperAdminMayReadAndMissingTokenIsUnauthorized() throws Exception {
        for (String role : List.of("PLATFORM_ADMIN", "SUPPORT_AGENT")) {
            String token = fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of(role));
            assertThat(send("GET", "/internal/audit", token, null).statusCode()).isEqualTo(403);
        }
        assertThat(send("GET", "/internal/audit", null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/internal/audit", "not-a-jwt", null).statusCode()).isEqualTo(401);
    }
}
