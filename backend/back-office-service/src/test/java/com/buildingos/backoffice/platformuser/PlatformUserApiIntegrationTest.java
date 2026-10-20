package com.buildingos.backoffice.platformuser;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.backoffice.support.JwtFixtures;
import com.buildingos.backoffice.support.UserAdminStub;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F6-T8c-BE (BOC-05): the users proxy relays the caller's token to a stubbed auth-service. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PlatformUserApiIntegrationTest {
    private static final String AUDIENCE = "backoffice-platform";
    private static final String BASE = "/api/v1/platform/backoffice/users";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;
    private static UserAdminStub auth;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("back_office_db").withUsername("back_office_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @BeforeAll
    static void start() throws Exception {
        fixtures = new JwtFixtures();
        auth = new UserAdminStub();
    }

    @AfterAll
    static void stop() {
        fixtures.close();
        auth.close();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> AUDIENCE);
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("buildingos.services.auth-url", auth::url);
        registry.add("buildingos.services.building-url", auth::url);
        registry.add("buildingos.services.subscription-url", auth::url);
    }

    @BeforeEach
    void reset() {
        auth.reset();
    }

    private String token(String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of(roles));
    }

    private HttpResponse<String> send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode json(HttpResponse<String> response) throws Exception {
        return JSON.readTree(response.body());
    }

    private UUID user(String... roles) {
        UUID id = UUID.randomUUID();
        auth.users.put(id, new TreeSet<>(List.of(roles)));
        return id;
    }

    @Test
    void operatorsListAndGetWithTheirTokenRelayed() throws Exception {
        UUID id = user("SUPPORT_AGENT");
        String token = token("PLATFORM_ADMIN");
        var list = send("GET", BASE + "?query=017&role=SUPPORT_AGENT&page=0&size=20", token, null);
        assertThat(list.statusCode()).as(list.body()).isEqualTo(200);
        assertThat(json(list).path("data").get(0).path("id").asString()).isEqualTo(id.toString());
        assertThat(json(list).path("meta").path("total").asInt()).isEqualTo(1);
        assertThat(auth.requests.get(0)).contains("query=017").contains("role=SUPPORT_AGENT");

        var one = send("GET", BASE + "/" + id, token, null);
        assertThat(json(one).path("data").path("platformRoles").get(0).asString()).isEqualTo("SUPPORT_AGENT");
        assertThat(auth.authorizations).allMatch(h -> h.equals("Bearer " + token));
    }

    @Test
    void superAdminAssignsAndRevokesRolesAndGetsTheUpdatedUser() throws Exception {
        UUID id = user("SUPPORT_AGENT");
        String token = token("SUPER_ADMIN");
        var assigned = send("POST", BASE + "/" + id + "/platform-roles", token, "{\"role\":\"ONBOARDING_AGENT\"}");
        assertThat(assigned.statusCode()).as(assigned.body()).isEqualTo(200);
        assertThat(json(assigned).path("data").path("platformRoles").toString())
                .contains("ONBOARDING_AGENT").contains("SUPPORT_AGENT");

        var revoked = send("DELETE", BASE + "/" + id + "/platform-roles/SUPPORT_AGENT", token, null);
        assertThat(revoked.statusCode()).as(revoked.body()).isEqualTo(200);
        assertThat(json(revoked).path("data").path("platformRoles").toString())
                .contains("ONBOARDING_AGENT").doesNotContain("SUPPORT_AGENT");
    }

    @Test
    void gatesByRoleBeforeAndAfterAuthService() throws Exception {
        UUID id = user();
        assertThat(send("GET", BASE, null, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", BASE, token("SUPPORT_AGENT"), null).statusCode()).isEqualTo(403);
        assertThat(send("POST", BASE + "/" + id + "/platform-roles", token("PLATFORM_ADMIN"),
                "{\"role\":\"SUPPORT_AGENT\"}").statusCode()).isEqualTo(403);
        assertThat(auth.requests).isEmpty();

        // auth-service has the last word: it refuses (e.g. a user changing their own roles).
        auth.forcedStatus = 403;
        var refused = send("POST", BASE + "/" + id + "/platform-roles", token("SUPER_ADMIN"), "{\"role\":\"SUPPORT_AGENT\"}");
        assertThat(refused.statusCode()).isEqualTo(403);
        assertThat(json(refused).path("code").asString()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void mapsValidationUnknownUsersAndOutages() throws Exception {
        String admin = token("SUPER_ADMIN");
        var badRole = send("POST", BASE + "/" + user() + "/platform-roles", admin, "{\"role\":\"ROOT\"}");
        assertThat(badRole.statusCode()).isEqualTo(400);
        assertThat(json(badRole).path("code").asString()).isEqualTo("INVALID_REQUEST");
        assertThat(send("GET", BASE + "?role=ROOT", admin, null).statusCode()).isEqualTo(400);

        var missing = send("GET", BASE + "/" + UUID.randomUUID(), admin, null);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(json(missing).path("code").asString()).isEqualTo("USER_NOT_FOUND");
        assertThat(send("DELETE", BASE + "/" + UUID.randomUUID() + "/platform-roles/SUPPORT_AGENT", admin, null)
                .statusCode()).isEqualTo(404);

        auth.forcedStatus = 500;
        var down = send("GET", BASE, admin, null);
        assertThat(down.statusCode()).isEqualTo(503);
        assertThat(json(down).path("code").asString()).isEqualTo("DEPENDENCY_UNAVAILABLE");
    }
}
