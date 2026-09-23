package com.buildingos.gateway;

import com.buildingos.gateway.support.JwtFixtures;
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

import static org.assertj.core.api.Assertions.assertThat;

/** Downstream unavailable must return a sanitized 503, never an internal URL/stack trace. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayDownstreamUnavailableTest {

    private static JwtFixtures fixtures;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String UNREACHABLE = "http://127.0.0.1:1";

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
        registry.add("platform.security.audience", () -> "gateway-platform");
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("ACCOUNT_SERVICE_URL", () -> UNREACHABLE);
        registry.add("BUILDING_SERVICE_URL", () -> UNREACHABLE);
        registry.add("SUBSCRIPTION_SERVICE_URL", () -> UNREACHABLE);
    }

    @Test
    void downstreamUnavailableReturnsSanitized503() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "gateway-platform", Instant.now().plusSeconds(300));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/platform/account"))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("SERVICE_UNAVAILABLE");
        assertThat(response.body()).doesNotContain(UNREACHABLE);
    }
}
