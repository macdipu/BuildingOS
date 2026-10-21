package com.buildingos.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.auth.support.JwtFixtures;
import com.nimbusds.jwt.SignedJWT;
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

/** BOS-010 D-38: client-credentials service token and the directory lookup it unlocks. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ServiceTokenIntegrationTest {
    private static final String AUDIENCE = "auth-platform";
    private static final String SECRET = "test-only-back-office-client-secret-0001";
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
        registry.add("buildingos.auth.service.clients.back-office-service.secret", () -> SECRET);
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

    private HttpResponse<String> exchange(String clientId, String secret) throws Exception {
        return send("POST", "/internal/service-tokens", null,
                "{\"clientId\":\"" + clientId + "\",\"clientSecret\":\"" + secret + "\"}");
    }

    private UUID userWithRole(String phone, String role) {
        UUID id = jdbc.queryForObject("INSERT INTO app_user (phone) VALUES (?) RETURNING id", UUID.class, phone);
        jdbc.update("INSERT INTO platform_user_role (user_id, role) VALUES (?, ?)", id, role);
        return id;
    }

    @Test
    void configuredClientGetsAShortLivedServiceTokenWithoutPlatformRoles() throws Exception {
        var reply = exchange("back-office-service", SECRET);
        assertThat(reply.statusCode()).isEqualTo(200);
        JsonNode data = JSON.readTree(reply.body()).path("data");
        assertThat(data.path("tokenType").asString()).isEqualTo("Bearer");
        assertThat(data.path("expiresIn").asLong()).isEqualTo(300);

        var claims = SignedJWT.parse(data.path("accessToken").asString()).getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo("service:back-office-service");
        assertThat(claims.getStringClaim("scope")).isEqualTo("directory.read");
        assertThat(claims.getClaim("platform_roles")).isNull();
        assertThat(claims.getClaim("phone")).isNull();
        assertThat(claims.getAudience()).containsExactly(AUDIENCE);
    }

    @Test
    void wrongSecretUnknownClientOrMissingFieldsAreRefused() throws Exception {
        var wrong = exchange("back-office-service", SECRET + "x");
        assertThat(wrong.statusCode()).isEqualTo(401);
        assertThat(JSON.readTree(wrong.body()).path("code").asString()).isEqualTo("INVALID_CLIENT");
        assertThat(wrong.body()).doesNotContain(SECRET);
        assertThat(exchange("someone-else", SECRET).statusCode()).isEqualTo(401);
        assertThat(send("POST", "/internal/service-tokens", null, "{\"clientId\":\"back-office-service\"}")
                .statusCode()).isEqualTo(400);
    }

    @Test
    void directoryReturnsRolesWithoutPersonalDataToServiceTokensOnly() throws Exception {
        UUID agent = userWithRole("01755600091", "SUPPORT_AGENT");
        String service = fixtures.serviceToken(AUDIENCE, "service:back-office-service", "directory.read");

        var found = send("GET", "/internal/directory/users/" + agent, service, null);
        assertThat(found.statusCode()).isEqualTo(200);
        JsonNode data = JSON.readTree(found.body()).path("data");
        assertThat(data.path("id").asString()).isEqualTo(agent.toString());
        assertThat(data.path("platformRoles").get(0).asString()).isEqualTo("SUPPORT_AGENT");
        assertThat(data.has("phone")).isFalse();

        var missing = send("GET", "/internal/directory/users/" + UUID.randomUUID(), service, null);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(missing.body()).path("code").asString()).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void userTokensAndServiceTokensWithoutTheScopeAreDenied() throws Exception {
        UUID agent = userWithRole("01755600092", "SUPPORT_AGENT");
        String path = "/internal/directory/users/" + agent;
        String superAdmin = fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of("SUPER_ADMIN"));
        String wrongScope = fixtures.serviceToken(AUDIENCE, "service:back-office-service", "audit.read");
        String userWithScope = fixtures.serviceToken(AUDIENCE, UUID.randomUUID().toString(), "directory.read");

        assertThat(send("GET", path, superAdmin, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", path, wrongScope, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", path, userWithScope, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", path, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void serviceTokenCannotManagePlatformRoles() throws Exception {
        UUID agent = userWithRole("01755600093", "SUPPORT_AGENT");
        String service = fixtures.serviceToken(AUDIENCE, "service:back-office-service", "directory.read");
        assertThat(send("GET", "/internal/users/" + agent, service, null).statusCode()).isEqualTo(403);
        assertThat(send("POST", "/internal/users/" + agent + "/platform-roles", service,
                "{\"role\":\"SUPER_ADMIN\"}").statusCode()).isEqualTo(403);
    }
}
