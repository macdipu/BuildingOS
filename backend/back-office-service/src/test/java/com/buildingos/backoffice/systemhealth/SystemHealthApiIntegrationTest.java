package com.buildingos.backoffice.systemhealth;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.backoffice.support.JwtFixtures;
import com.buildingos.backoffice.support.ReadinessStub;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/** F6-T5a (BOC-08): readiness aggregation against stubbed auth/building/subscription plus this service itself. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class SystemHealthApiIntegrationTest {
    private static final String AUDIENCE = "backoffice-platform";
    private static final String PATH = "/api/v1/platform/backoffice/system-health";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;
    private static ReadinessStub auth;
    private static ReadinessStub building;
    private static ReadinessStub subscription;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("back_office_db").withUsername("back_office_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
        auth = new ReadinessStub();
        building = new ReadinessStub();
        subscription = new ReadinessStub();
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
        auth.close();
        building.close();
        subscription.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> AUDIENCE);
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("buildingos.services.auth-url", auth::url);
        registry.add("buildingos.services.building-url", building::url);
        registry.add("buildingos.services.subscription-url", subscription::url);
        registry.add("buildingos.health.timeout", () -> "PT0.5S");
    }

    @BeforeEach
    void reset() {
        auth.reset();
        building.reset();
        subscription.reset();
    }

    private HttpResponse<String> get(String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + PATH)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode healthAsSuperAdmin() throws Exception {
        var response = get(fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of("SUPER_ADMIN")));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).doesNotContain("SECRET-DETAIL").doesNotContain("components");
        return JSON.readTree(response.body()).path("data");
    }

    private static Map<String, JsonNode> byName(JsonNode data) {
        Map<String, JsonNode> services = new HashMap<>();
        data.path("services").forEach(service -> services.put(service.path("name").asString(), service));
        return services;
    }

    @Test
    void allUpGivesOverallUpIncludingOwnReadiness() throws Exception {
        var data = healthAsSuperAdmin();
        assertThat(data.path("overall").asString()).isEqualTo("UP");
        assertThat(data.path("checkedAt").asString()).isNotBlank();
        var services = byName(data);
        assertThat(services).containsOnlyKeys("auth-service", "building-service", "subscription-service",
                "back-office-service");
        services.values().forEach(service -> {
            assertThat(service.path("status").asString()).isEqualTo("UP");
            assertThat(service.path("httpStatus").asInt()).isEqualTo(200);
            assertThat(service.path("latencyMs").isNumber()).isTrue();
        });
    }

    @Test
    void oneServiceAnswering503IsDownAndOverallDegraded() throws Exception {
        building.status = 503;
        var data = healthAsSuperAdmin();
        assertThat(data.path("overall").asString()).isEqualTo("DEGRADED");
        var buildingService = byName(data).get("building-service");
        assertThat(buildingService.path("status").asString()).isEqualTo("DOWN");
        assertThat(buildingService.path("httpStatus").asInt()).isEqualTo(503);
        assertThat(byName(data).get("auth-service").path("status").asString()).isEqualTo("UP");
    }

    @Test
    void timedOutServiceIsUnknownAndEndpointStillAnswers() throws Exception {
        subscription.delayMs = 3000;
        var data = healthAsSuperAdmin();
        assertThat(data.path("overall").asString()).isEqualTo("DEGRADED");
        var subscriptionService = byName(data).get("subscription-service");
        assertThat(subscriptionService.path("status").asString()).isEqualTo("UNKNOWN");
        assertThat(subscriptionService.has("httpStatus")).isFalse();
    }

    @Test
    void nonSuperAdminIsForbidden() throws Exception {
        for (String role : List.of("PLATFORM_ADMIN", "SUPPORT_AGENT", "ONBOARDING_AGENT")) {
            var response = get(fixtures.userToken(AUDIENCE, UUID.randomUUID(), List.of(role)));
            assertThat(response.statusCode()).as(role).isEqualTo(403);
            assertThat(JSON.readTree(response.body()).path("code").asString()).isEqualTo("ACCESS_DENIED");
        }
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        assertThat(get(null).statusCode()).isEqualTo(401);
    }
}
