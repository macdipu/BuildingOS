package com.buildingos.gateway;

import com.buildingos.gateway.support.JwtFixtures;
import com.buildingos.gateway.support.StubDownstream;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingTest {

    private static JwtFixtures fixtures;
    private static StubDownstream identityStub;
    private static StubDownstream buildingStub;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
        identityStub = new StubDownstream("identity-service");
        buildingStub = new StubDownstream("building-service");
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
        identityStub.close();
        buildingStub.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> "gateway-platform");
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("IDENTITY_SERVICE_URL", () -> identityStub.baseUrl);
        registry.add("BUILDING_SERVICE_URL", () -> buildingStub.baseUrl);
    }

    private HttpResponse<String> call(String path, String bearerToken, String extraHeaderName, String extraHeaderValue)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        if (extraHeaderName != null) {
            builder.header(extraHeaderName, extraHeaderValue);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String validToken() throws Exception {
        return fixtures.token(fixtures.issuer(), "gateway-platform", Instant.now().plusSeconds(300));
    }

    @Test
    void validTokenForwardsToIdentityService() throws Exception {
        var response = call("/api/v1/platform/identity", validToken(), null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"identity-service\"");
        assertThat(identityStub.lastCorrelationHeader()).isNotBlank();
    }

    @Test
    void validTokenForwardsToBuildingService() throws Exception {
        var response = call("/api/v1/platform/building", validToken(), null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"building-service\"");
        assertThat(buildingStub.lastCorrelationHeader()).isNotBlank();
    }

    @Test
    void correlationIdIsPropagatedAndReturned() throws Exception {
        var response = call("/api/v1/platform/identity", validToken(), null, null);
        String returned = response.headers().firstValue("X-Correlation-Id").orElse(null);
        assertThat(returned).isNotBlank();
        assertThat(identityStub.lastCorrelationHeader()).isEqualTo(returned);
    }

    @Test
    void missingTokenRejectedAtGateway() throws Exception {
        assertThat(call("/api/v1/platform/identity", null, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void expiredTokenRejectedAtGateway() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "gateway-platform", Instant.now().minusSeconds(600));
        assertThat(call("/api/v1/platform/identity", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void wrongAudienceRejectedAtGateway() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "wrong-audience", Instant.now().plusSeconds(300));
        assertThat(call("/api/v1/platform/identity", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void wrongIssuerRejectedAtGateway() throws Exception {
        String token = fixtures.token("http://wrong-issuer.invalid", "gateway-platform", Instant.now().plusSeconds(300));
        assertThat(call("/api/v1/platform/identity", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void badSignatureRejectedAtGateway() throws Exception {
        String token = fixtures.tokenSignedByOtherKey(fixtures.issuer(), "gateway-platform");
        assertThat(call("/api/v1/platform/identity", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void spoofedHeaderDoesNotGrantAccess() throws Exception {
        assertThat(call("/api/v1/platform/identity", null, "X-Building-Id", "some-building").statusCode())
                .isEqualTo(401);
    }

    @Test
    void otpStartIsPubliclyReachableWithoutAToken() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/auth/otp/start"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"phone\":\"+8801700000000\"}"))
                .header("Content-Type", "application/json")
                .build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("otp-start");
        assertThat(identityStub.lastCorrelationHeader()).isNotBlank();
    }
}
