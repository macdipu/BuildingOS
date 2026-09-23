package com.buildingos.account.auth;

import com.buildingos.account.support.JwtFixtures;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end proof of the local/test OTP login slice: start -> verify with the fixed
 * development code -> a JWT signed by this instance's own local issuer, validated
 * against its own published JWKS. The seeded SUPER_ADMIN phone authenticates the same
 * way as any other user and carries the platform_roles claim.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OtpLoginIntegrationTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String SUPER_ADMIN_PHONE = "01306999005";
    private static final String OTHER_PHONE = "+8801711112222";

    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("account_db").withUsername("account_app").withPassword("test-only-password");

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
        registry.add("platform.security.audience", () -> "account-platform");
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private String base() {
        return "http://127.0.0.1:" + port;
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private UUID startChallenge(String phone) throws Exception {
        var response = post("/api/v1/auth/otp/start", "{\"phone\":\"" + phone + "\"}");
        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        String idString = body.replaceAll(".*\"attemptId\":\"([0-9a-fA-F-]+)\".*", "$1");
        return UUID.fromString(idString);
    }

    private JwtDecoder localIssuerDecoder() throws Exception {
        var jwksResponse = HTTP.send(HttpRequest.newBuilder(URI.create(base() + "/.well-known/jwks.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(jwksResponse.statusCode()).isEqualTo(200);
        JWKSet jwkSet = JWKSet.parse(jwksResponse.body());
        return buildDecoderFromJwkSet(jwkSet);
    }

    private JwtDecoder buildDecoderFromJwkSet(JWKSet jwkSet) {
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(jwkSet);
        var jwtProcessor = new DefaultJWTProcessor<SecurityContext>();
        var keySelector = new com.nimbusds.jose.proc.JWSVerificationKeySelector<SecurityContext>(
                com.nimbusds.jose.JWSAlgorithm.RS256, source);
        jwtProcessor.setJWSKeySelector(keySelector);
        return new NimbusJwtDecoder(jwtProcessor);
    }

    @Test
    void seededSuperAdminCanLoginWithDevelopmentCodeAndGetsPlatformRoleClaim() throws Exception {
        UUID attemptId = startChallenge(SUPER_ADMIN_PHONE);
        var verifyResponse = post("/api/v1/auth/otp/verify",
                "{\"attemptId\":\"" + attemptId + "\",\"phone\":\"" + SUPER_ADMIN_PHONE + "\",\"code\":\"000000\"}");
        assertThat(verifyResponse.statusCode()).isEqualTo(200);
        assertThat(verifyResponse.body()).contains("SUPER_ADMIN");

        String token = verifyResponse.body().replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
        var decoded = localIssuerDecoder().decode(token);
        assertThat(decoded.getClaimAsStringList("platform_roles")).contains("SUPER_ADMIN");
        assertThat(decoded.getSubject()).isNotBlank();
    }

    @Test
    void unseededPhoneGetsNoRoleClaimsButStillAuthenticates() throws Exception {
        UUID attemptId = startChallenge(OTHER_PHONE);
        var verifyResponse = post("/api/v1/auth/otp/verify",
                "{\"attemptId\":\"" + attemptId + "\",\"phone\":\"" + OTHER_PHONE + "\",\"code\":\"000000\"}");
        assertThat(verifyResponse.statusCode()).isEqualTo(200);
        assertThat(verifyResponse.body()).doesNotContain("SUPER_ADMIN");
    }

    @Test
    void wrongCodeIsRejectedWithUnauthorized() throws Exception {
        UUID attemptId = startChallenge("+8801711113333");
        var verifyResponse = post("/api/v1/auth/otp/verify",
                "{\"attemptId\":\"" + attemptId + "\",\"phone\":\"+8801711113333\",\"code\":\"111111\"}");
        assertThat(verifyResponse.statusCode()).isEqualTo(401);
        assertThat(verifyResponse.body()).contains("OTP_INVALID_CODE");
    }

    @Test
    void repeatedStartReturnsRateLimited() throws Exception {
        startChallenge("+8801711114444");
        var response = post("/api/v1/auth/otp/start", "{\"phone\":\"+8801711114444\"}");
        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(response.body()).contains("OTP_RATE_LIMITED");
    }

    @Test
    void rateLimitIsSharedAcrossPhoneForms() throws Exception {
        startChallenge("+8801711115555");
        var response = post("/api/v1/auth/otp/start", "{\"phone\":\"01711115555\"}");
        assertThat(response.statusCode()).isEqualTo(429);
    }

    @Test
    void nonBangladeshMobileNumberIsRejectedAtStart() throws Exception {
        var response = post("/api/v1/auth/otp/start", "{\"phone\":\"+14155550123\"}");
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("INVALID_REQUEST");
    }

    @Test
    void jwksEndpointIsPubliclyReachableWithoutAToken() throws Exception {
        var response = HTTP.send(HttpRequest.newBuilder(URI.create(base() + "/.well-known/jwks.json")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"keys\"");
    }
}
