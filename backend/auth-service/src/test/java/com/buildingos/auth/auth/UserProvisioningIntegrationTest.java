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

/** D-29: building-service provisions the initial building admin by phone with a relayed platform-admin token. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class UserProvisioningIntegrationTest {
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

    private HttpResponse<String> provision(String token, String phone) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/internal/users/provision"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"phone\":\"" + phone + "\"}"));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String token(String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of(roles));
    }

    @Test
    void createsOnceThenReturnsTheSameUser() throws Exception {
        var first = provision(token("PLATFORM_ADMIN"), "+8801755500011");
        assertThat(first.statusCode()).isEqualTo(200);
        JsonNode data = JSON.readTree(first.body()).path("data");
        assertThat(data.path("phone").asString()).isEqualTo("01755500011");
        var second = provision(token("SUPER_ADMIN"), "01755500011");
        assertThat(JSON.readTree(second.body()).path("data").path("userId").asString())
                .isEqualTo(data.path("userId").asString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE phone = '01755500011'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM platform_user_role r JOIN app_user u ON u.id = r.user_id "
                + "WHERE u.phone = '01755500011'", Integer.class)).isZero();
    }

    @Test
    void requiresPlatformAdmin() throws Exception {
        assertThat(provision(token(), "01755500022").statusCode()).isEqualTo(403);
        assertThat(provision(token("SUBSCRIPTION_ADMIN"), "01755500022").statusCode()).isEqualTo(403);
        assertThat(provision(null, "01755500022").statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE phone = '01755500022'", Integer.class))
                .isZero();
    }

    @Test
    void rejectsInvalidPhone() throws Exception {
        assertThat(provision(token("PLATFORM_ADMIN"), "12345").statusCode()).isEqualTo(400);
    }
}
