package com.buildingos.building.platform;

import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PlatformMetadataSecurityTest {

    private static JwtFixtures fixtures;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

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
        registry.add("platform.security.audience", () -> "building-platform");
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private HttpResponse<String> call(String bearerToken, String extraHeaderName, String extraHeaderValue) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/internal/platform/info"))
                .GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        if (extraHeaderName != null) {
            builder.header(extraHeaderName, extraHeaderValue);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void validTokenReturnsMetadata() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "building-platform", Instant.now().plusSeconds(300));
        var response = call(token, null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"building-service\"");
    }

    @Test
    void missingTokenRejected() throws Exception {
        var response = call(null, null, null);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void expiredTokenRejected() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "building-platform", Instant.now().minusSeconds(600));
        var response = call(token, null, null);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void wrongAudienceRejected() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "wrong-audience", Instant.now().plusSeconds(300));
        var response = call(token, null, null);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void wrongIssuerRejected() throws Exception {
        String token = fixtures.token("http://wrong-issuer.invalid", "building-platform", Instant.now().plusSeconds(300));
        var response = call(token, null, null);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void badSignatureRejected() throws Exception {
        String token = fixtures.tokenSignedByOtherKey(fixtures.issuer(), "building-platform");
        var response = call(token, null, null);
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void spoofedHeaderDoesNotGrantAccess() throws Exception {
        var response = call(null, "X-Building-Id", "some-building");
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
