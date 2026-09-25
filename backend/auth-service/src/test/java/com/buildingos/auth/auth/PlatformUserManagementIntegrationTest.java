package com.buildingos.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.auth.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

/** F6-T2 (BOC-05): back-office-service lists platform users and grants/revokes platform roles via auth-service. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PlatformUserManagementIntegrationTest {
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

    private String token(UUID subject, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, subject, List.of(roles));
    }

    private String token(String... roles) throws Exception {
        return token(UUID.randomUUID(), roles);
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

    private HttpResponse<String> assign(String token, UUID target, String role) throws Exception {
        return send("POST", "/internal/users/" + target + "/platform-roles", token, "{\"role\":\"" + role + "\"}");
    }

    private HttpResponse<String> revoke(String token, UUID target, String role) throws Exception {
        return send("DELETE", "/internal/users/" + target + "/platform-roles/" + role, token, null);
    }

    private int roleCount(UUID userId, String role) {
        return jdbc.queryForObject("SELECT count(*) FROM platform_user_role WHERE user_id = ? AND role = ?",
                Integer.class, userId, role);
    }

    @Test
    void superAdminListsViewsAssignsAndRevokes() throws Exception {
        UUID target = user("01755600011");
        String superAdmin = token("SUPER_ADMIN");

        var listed = send("GET", "/internal/users?query=01755600011", superAdmin, null);
        assertThat(listed.statusCode()).isEqualTo(200);
        JsonNode listBody = JSON.readTree(listed.body());
        assertThat(listBody.path("data")).hasSize(1);
        assertThat(listBody.path("data").get(0).path("id").asString()).isEqualTo(target.toString());
        assertThat(listBody.path("meta").path("total").asLong()).isEqualTo(1);

        var assigned = assign(superAdmin, target, "SUPPORT_AGENT");
        assertThat(assigned.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(assigned.body()).path("data").path("platformRoles").get(0).asString())
                .isEqualTo("SUPPORT_AGENT");

        var byRole = send("GET", "/internal/users?role=SUPPORT_AGENT", superAdmin, null);
        assertThat(JSON.readTree(byRole.body()).path("data").findValuesAsString("id")).contains(target.toString());

        var viewed = send("GET", "/internal/users/" + target, superAdmin, null);
        assertThat(viewed.statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(viewed.body()).path("data").path("phone").asString()).isEqualTo("01755600011");

        assertThat(revoke(superAdmin, target, "SUPPORT_AGENT").statusCode()).isEqualTo(204);
        assertThat(roleCount(target, "SUPPORT_AGENT")).isZero();
    }

    @Test
    void platformAdminMayReadButNotChangeRoles() throws Exception {
        UUID target = user("01755600022");
        String platformAdmin = token("PLATFORM_ADMIN");
        assertThat(send("GET", "/internal/users", platformAdmin, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/internal/users/" + target, platformAdmin, null).statusCode()).isEqualTo(200);
        assertThat(assign(platformAdmin, target, "SUPPORT_AGENT").statusCode()).isEqualTo(403);
        assertThat(revoke(platformAdmin, target, "SUPPORT_AGENT").statusCode()).isEqualTo(403);
        assertThat(roleCount(target, "SUPPORT_AGENT")).isZero();
    }

    @Test
    void otherRolesAreForbiddenAndMissingTokenIsUnauthorized() throws Exception {
        UUID target = user("01755600033");
        for (String caller : List.of(token(), token("SUBSCRIPTION_ADMIN"), token("SUPPORT_AGENT"))) {
            assertThat(send("GET", "/internal/users", caller, null).statusCode()).isEqualTo(403);
            assertThat(send("GET", "/internal/users/" + target, caller, null).statusCode()).isEqualTo(403);
            assertThat(assign(caller, target, "SUPPORT_AGENT").statusCode()).isEqualTo(403);
            assertThat(revoke(caller, target, "SUPPORT_AGENT").statusCode()).isEqualTo(403);
        }
        assertThat(send("GET", "/internal/users", null, null).statusCode()).isEqualTo(401);
        assertThat(assign("not-a-jwt", target, "SUPPORT_AGENT").statusCode()).isEqualTo(401);
    }

    @Test
    void superAdminCannotChangeOwnRoles() throws Exception {
        UUID self = user("01755600044");
        jdbc.update("INSERT INTO platform_user_role (user_id, role) VALUES (?, 'SUPER_ADMIN')", self);
        String selfToken = token(self, "SUPER_ADMIN");
        assertThat(assign(selfToken, self, "PLATFORM_ADMIN").statusCode()).isEqualTo(403);
        assertThat(revoke(selfToken, self, "SUPER_ADMIN").statusCode()).isEqualTo(403);
        assertThat(roleCount(self, "SUPER_ADMIN")).isEqualTo(1);
        assertThat(roleCount(self, "PLATFORM_ADMIN")).isZero();
    }

    @Test
    void repeatedAssignAndRevokeAreNoOps() throws Exception {
        UUID target = user("01755600055");
        String superAdmin = token("SUPER_ADMIN");
        assertThat(assign(superAdmin, target, "ONBOARDING_AGENT").statusCode()).isEqualTo(200);
        assertThat(assign(superAdmin, target, "ONBOARDING_AGENT").statusCode()).isEqualTo(200);
        assertThat(roleCount(target, "ONBOARDING_AGENT")).isEqualTo(1);
        assertThat(revoke(superAdmin, target, "ONBOARDING_AGENT").statusCode()).isEqualTo(204);
        assertThat(revoke(superAdmin, target, "ONBOARDING_AGENT").statusCode()).isEqualTo(204);
        assertThat(roleCount(target, "ONBOARDING_AGENT")).isZero();
    }

    @Test
    void unknownUserIsNotFoundAndUnknownRoleIsBadRequest() throws Exception {
        String superAdmin = token("SUPER_ADMIN");
        UUID missing = UUID.randomUUID();
        assertThat(send("GET", "/internal/users/" + missing, superAdmin, null).statusCode()).isEqualTo(404);
        assertThat(assign(superAdmin, missing, "SUPPORT_AGENT").statusCode()).isEqualTo(404);
        assertThat(revoke(superAdmin, missing, "SUPPORT_AGENT").statusCode()).isEqualTo(404);
        assertThat(assign(superAdmin, user("01755600066"), "NOT_A_ROLE").statusCode()).isEqualTo(400);
    }
}
