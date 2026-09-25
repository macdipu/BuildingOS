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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    private static StubDownstream authStub;
    private static StubDownstream buildingStub;
    private static StubDownstream subscriptionStub;
    private static StubDownstream backOfficeStub;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
        authStub = new StubDownstream("auth-service");
        buildingStub = new StubDownstream("building-service");
        subscriptionStub = new StubDownstream("subscription-service");
        backOfficeStub = new StubDownstream("back-office-service");
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
        authStub.close();
        buildingStub.close();
        subscriptionStub.close();
        backOfficeStub.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> "gateway-platform");
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("AUTH_SERVICE_URL", () -> authStub.baseUrl);
        registry.add("BUILDING_SERVICE_URL", () -> buildingStub.baseUrl);
        registry.add("SUBSCRIPTION_SERVICE_URL", () -> subscriptionStub.baseUrl);
        registry.add("BACK_OFFICE_SERVICE_URL", () -> backOfficeStub.baseUrl);
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
    void validTokenForwardsToAuthService() throws Exception {
        var response = call("/api/v1/platform/auth", validToken(), null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"auth-service\"");
        assertThat(authStub.lastCorrelationHeader()).isNotBlank();
    }

    @Test
    void validTokenForwardsToBuildingService() throws Exception {
        var response = call("/api/v1/platform/building", validToken(), null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"building-service\"");
        assertThat(buildingStub.lastCorrelationHeader()).isNotBlank();
    }

    @Test
    void validTokenForwardsToSubscriptionServiceMetadata() throws Exception {
        var response = call("/api/v1/platform/subscription", validToken(), null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"subscription-service\"");
    }

    @Test
    void validTokenForwardsToBackOfficeServiceMetadata() throws Exception {
        var response = call("/api/v1/platform/backoffice", validToken(), null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"back-office-service\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/platform/backoffice/support-sessions",
            "/api/v1/platform/backoffice/onboarding-sessions/abc"})
    void backOfficeApiPathsForwardUnchangedWithBearerToken(String path) throws Exception {
        String token = validToken();
        var response = call(path, token, null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"back-office-service\"", "\"path\":\"" + path + "\"");
        assertThat(backOfficeStub.lastAuthorization()).isEqualTo("Bearer " + token);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/platform/subscription-plans", "/api/v1/platform/subscription-plans/abc",
            "/api/v1/platform/free-tier", "/api/v1/platform/users/abc/subscription",
            "/api/v1/platform/fees/BUILDING_CREATION/status", "/api/v1/me/plans", "/api/v1/me/entitlements"})
    void revenueApiPathsForwardUnchangedWithBearerToken(String path) throws Exception {
        String token = validToken();
        var response = call(path, token, null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"subscription-service\"", "\"path\":\"" + path + "\"");
        assertThat(subscriptionStub.lastAuthorization()).isEqualTo("Bearer " + token);
    }

    @Test
    void revenueWritesForwardPutAndPost() throws Exception {
        for (String method : new String[] {"PUT", "POST"}) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/platform/free-tier"))
                    .method(method, HttpRequest.BodyPublishers.ofString("{}"))
                    .header("Authorization", "Bearer " + validToken())
                    .header("Content-Type", "application/json").build();
            var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"method\":\"" + method + "\"");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/building-applications", "/api/v1/building-applications/abc/submit",
            "/api/v1/building-applications/abc/documents/def", "/api/v1/me/building-applications",
            "/api/v1/platform/building-applications", "/api/v1/platform/building-applications/abc/duplicates",
            "/api/v1/platform/buildings/abc", "/api/v1/platform/buildings/abc/activate"})
    void buildingApiPathsForwardUnchangedWithBearerToken(String path) throws Exception {
        String token = validToken();
        var response = call(path, token, null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"building-service\"", "\"path\":\"" + path + "\"");
        assertThat(buildingStub.lastAuthorization()).isEqualTo("Bearer " + token);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/me/buildings", "/api/v1/me/properties", "/api/v1/me/building-invitations",
            "/api/v1/me/building-invitations/abc/claim", "/api/v1/buildings/abc", "/api/v1/buildings/abc/floors",
            "/api/v1/buildings/abc/units/def", "/api/v1/buildings/abc/unit-batches/preview",
            "/api/v1/buildings/abc/invitations/def/revoke", "/api/v1/buildings/abc/members",
            "/api/v1/buildings/abc/units/def/ownership-history",
            "/api/v1/buildings/abc/units/def/ownership-transfers/ghi/documents/jkl"})
    void unitsAndOwnershipPathsForwardUnchangedWithBearerToken(String path) throws Exception {
        String token = validToken();
        var response = call(path, token, null, null);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"service\":\"building-service\"", "\"path\":\"" + path + "\"");
        assertThat(buildingStub.lastAuthorization()).isEqualTo("Bearer " + token);
    }

    @Test
    void unitsAndOwnershipWritesForwardEveryMethod() throws Exception {
        for (String method : new String[] {"POST", "PUT", "DELETE"}) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                            + "/api/v1/buildings/abc/units/def/ownership-transfers/ghi/documents/jkl"))
                    .method(method, HttpRequest.BodyPublishers.ofString("{\"reason\":\"x\"}"))
                    .header("Authorization", "Bearer " + validToken())
                    .header("Content-Type", "application/json").build();
            var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"method\":\"" + method + "\"");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/me/properties", "/api/v1/buildings/abc/units"})
    void unitsAndOwnershipRequireTokenAtGateway(String path) throws Exception {
        buildingStub.reset();
        assertThat(call(path, null, null, null).statusCode()).isEqualTo(401);
        assertThat(buildingStub.lastAuthorization()).isNull();
    }

    @Test
    void documentUploadStreamsMultipartBodyIntact() throws Exception {
        byte[] file = new byte[2 * 1024 * 1024];
        new java.util.Random(7).nextBytes(file);
        String boundary = "----gw" + java.util.UUID.randomUUID();
        var body = new java.io.ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"deed.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        body.write(file);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/api/v1/building-applications/abc/documents"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .header("Authorization", "Bearer " + validToken())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary).build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(buildingStub.lastBody()).isEqualTo(body.toByteArray());
        assertThat(buildingStub.lastContentType()).startsWith("multipart/form-data").contains("boundary=" + boundary);
    }

    @Test
    void internalPathsAreNeverRouted() throws Exception {
        for (String path : new String[] {"/internal/users/provision", "/internal/platform/info"}) {
            var response = call(path, validToken(), null, null);
            assertThat(response.statusCode()).isNotEqualTo(200);
            assertThat(response.body()).doesNotContain("auth-service", "building-service");
        }
    }

    @Test
    void revenueApiRequiresTokenAtGateway() throws Exception {
        assertThat(call("/api/v1/me/entitlements", null, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void correlationIdIsPropagatedAndReturned() throws Exception {
        var response = call("/api/v1/platform/auth", validToken(), null, null);
        String returned = response.headers().firstValue("X-Correlation-Id").orElse(null);
        assertThat(returned).isNotBlank();
        assertThat(authStub.lastCorrelationHeader()).isEqualTo(returned);
    }

    @Test
    void missingTokenRejectedAtGateway() throws Exception {
        assertThat(call("/api/v1/platform/auth", null, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void expiredTokenRejectedAtGateway() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "gateway-platform", Instant.now().minusSeconds(600));
        assertThat(call("/api/v1/platform/auth", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void wrongAudienceRejectedAtGateway() throws Exception {
        String token = fixtures.token(fixtures.issuer(), "wrong-audience", Instant.now().plusSeconds(300));
        assertThat(call("/api/v1/platform/auth", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void wrongIssuerRejectedAtGateway() throws Exception {
        String token = fixtures.token("http://wrong-issuer.invalid", "gateway-platform", Instant.now().plusSeconds(300));
        assertThat(call("/api/v1/platform/auth", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void badSignatureRejectedAtGateway() throws Exception {
        String token = fixtures.tokenSignedByOtherKey(fixtures.issuer(), "gateway-platform");
        assertThat(call("/api/v1/platform/auth", token, null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void spoofedHeaderDoesNotGrantAccess() throws Exception {
        assertThat(call("/api/v1/platform/auth", null, "X-Building-Id", "some-building").statusCode())
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
        assertThat(authStub.lastCorrelationHeader()).isNotBlank();
    }
}
