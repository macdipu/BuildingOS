package com.buildingos.building.building;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.DownstreamStubs;
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

/** BOS-010 D-38: building existence lookup for service tokens with {@code directory.read} only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class DirectoryBuildingIntegrationTest {
    private static final String AUDIENCE = "building-platform";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;
    private static DownstreamStubs stubs;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
        stubs = new DownstreamStubs();
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
        stubs.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> AUDIENCE);
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("buildingos.services.auth-url", stubs::url);
        registry.add("buildingos.services.subscription-url", stubs::url);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private UUID building(String status) {
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
                id, application, "Directory " + id, status, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    @Test
    void serviceTokenWithDirectoryReadSeesIdAndStatusOnly() throws Exception {
        UUID id = building("ONBOARDING");
        String service = fixtures.serviceToken(AUDIENCE, "service:back-office-service", "directory.read");

        var found = get("/internal/directory/buildings/" + id, service);
        assertThat(found.statusCode()).isEqualTo(200);
        JsonNode data = JSON.readTree(found.body()).path("data");
        assertThat(data.path("id").asString()).isEqualTo(id.toString());
        assertThat(data.path("status").asString()).isEqualTo("ONBOARDING");
        assertThat(data.has("name")).isFalse();

        var missing = get("/internal/directory/buildings/" + UUID.randomUUID(), service);
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(JSON.readTree(missing.body()).path("code").asString()).isEqualTo("BUILDING_NOT_FOUND");
    }

    @Test
    void userTokensAndServiceTokensWithoutTheScopeAreDenied() throws Exception {
        String path = "/internal/directory/buildings/" + building("ACTIVE");
        String superAdmin = fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of("SUPER_ADMIN"));
        String wrongScope = fixtures.serviceToken(AUDIENCE, "service:back-office-service", "audit.read");
        String userWithScope = fixtures.serviceToken(AUDIENCE, UUID.randomUUID().toString(), "directory.read");

        assertThat(get(path, superAdmin).statusCode()).isEqualTo(403);
        assertThat(get(path, wrongScope).statusCode()).isEqualTo(403);
        assertThat(get(path, userWithScope).statusCode()).isEqualTo(403);
        assertThat(get(path, null).statusCode()).isEqualTo(401);
    }
}
