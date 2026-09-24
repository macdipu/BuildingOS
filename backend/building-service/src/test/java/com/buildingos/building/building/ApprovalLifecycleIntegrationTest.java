package com.buildingos.building.building;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.DownstreamStubs;
import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

/** AP-07..AP-09: approval (fee check, admin provisioning), building lifecycle, duplicate signals. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ApprovalLifecycleIntegrationTest {
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

    private final UUID applicant = UUID.randomUUID();
    private final UUID reviewer = UUID.randomUUID();

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

    @BeforeEach
    void resetStubs() {
        stubs.reset();
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

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private String token(UUID user, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of(roles));
    }

    private String reviewerToken() throws Exception { return token(reviewer, "PLATFORM_ADMIN"); }

    private Reply send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private static String details(String name, String address, String phone) {
        return "{\"buildingName\":\"" + name + "\",\"buildingType\":\"RESIDENTIAL\",\"address\":\"" + address
                + "\",\"area\":\"Mirpur\",\"district\":\"Dhaka\",\"estimatedUnits\":24,"
                + "\"applicantRelationship\":\"OWNER\",\"contactName\":\"Karim\",\"contactPhone\":\"" + phone + "\"}";
    }

    private String underReview(String name, String address, String phone) throws Exception {
        String id = send("POST", "/api/v1/building-applications", token(applicant), details(name, address, phone))
                .data().path("id").asString();
        assertThat(send("POST", "/api/v1/building-applications/" + id + "/submit", token(applicant), null).status())
                .isEqualTo(200);
        assertThat(send("POST", "/api/v1/platform/building-applications/" + id + "/start-review", reviewerToken(),
                null).status()).isEqualTo(200);
        return id;
    }

    private String underReview() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return underReview("Tower " + unique, "Plot " + unique, "01911" + (100000 + (int) (Math.random() * 899999)));
    }

    private Reply approve(String id, String body) throws Exception {
        return send("POST", "/api/v1/platform/building-applications/" + id + "/approve", reviewerToken(), body);
    }

    private static final String APPROVE = "{\"adminPhone\":\"+8801812345678\",\"reason\":\"Deed verified\"}";

    private String status(String id) throws Exception {
        return send("GET", "/api/v1/building-applications/" + id, reviewerToken(), null).data().path("status").asString();
    }

    private int buildingsFor(String id) {
        return jdbc.queryForObject("SELECT count(*) FROM building WHERE application_id = ?::uuid", Integer.class, id);
    }

    @Test
    void approvalCreatesOnboardingBuildingWithProvisionedAdmin() throws Exception {
        String id = underReview();
        var approved = approve(id, APPROVE);
        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.data().path("application").path("status").asString()).isEqualTo("APPROVED");
        assertThat(approved.data().path("buildingStatus").asString()).isEqualTo("ONBOARDING");
        String buildingId = approved.data().path("buildingId").asString();

        assertThat(stubs.feeAuthorizations).hasSize(1);
        assertThat(stubs.provisionBodies).containsExactly("{\"phone\":\"01812345678\"}");
        assertThat(stubs.provisionAuthorizations).allMatch(h -> h != null && h.startsWith("Bearer "));

        var building = send("GET", "/api/v1/platform/buildings/" + buildingId, reviewerToken(), null).data();
        assertThat(building.path("status").asString()).isEqualTo("ONBOARDING");
        assertThat(building.path("members").get(0).path("userId").asString())
                .isEqualTo(stubs.provisionedUserId.toString());
        assertThat(building.path("members").get(0).path("role").asString()).isEqualTo("BUILDING_ADMIN");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lifecycle_transition WHERE entity_type = 'BUILDING' "
                + "AND entity_id = ?::uuid AND to_status = 'ONBOARDING' AND reason = 'Deed verified'", Integer.class,
                buildingId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lifecycle_transition WHERE entity_id = ?::uuid "
                + "AND to_status = 'APPROVED' AND actor_user_id = ?", Integer.class, id, reviewer)).isEqualTo(1);

        var again = approve(id, APPROVE);
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("INVALID_TRANSITION");
        assertThat(buildingsFor(id)).isEqualTo(1);
    }

    @Test
    void approvalRelaysTheApproversToken() throws Exception {
        String id = underReview();
        String token = reviewerToken();
        send("POST", "/api/v1/platform/building-applications/" + id + "/approve", token, APPROVE);
        assertThat(stubs.feeAuthorizations).containsExactly("Bearer " + token);
        assertThat(stubs.provisionAuthorizations).containsExactly("Bearer " + token);
    }

    @Test
    void unpaidOrUnconfiguredFeeBlocksApprovalWithoutChanges() throws Exception {
        String id = underReview();
        stubs.feeReply.set(DownstreamStubs.fee("UNPAID"));
        var unpaid = approve(id, APPROVE);
        assertThat(unpaid.status()).isEqualTo(409);
        assertThat(unpaid.code()).isEqualTo("CREATION_FEE_UNPAID");
        assertThat(stubs.provisionBodies).isEmpty();

        stubs.feeReply.set(DownstreamStubs.feeNotConfigured());
        assertThat(approve(id, APPROVE).code()).isEqualTo("FEE_NOT_CONFIGURED");

        stubs.feeReply.set(DownstreamStubs.fee("NOT_REQUIRED"));
        assertThat(status(id)).isEqualTo("UNDER_REVIEW");
        assertThat(buildingsFor(id)).isZero();
        assertThat(approve(id, APPROVE).status()).isEqualTo(200);
    }

    @Test
    void unavailableDependenciesAre503AndChangeNothing() throws Exception {
        String id = underReview();
        stubs.feeReply.set(new DownstreamStubs.Reply(500, "{}"));
        var feeDown = approve(id, APPROVE);
        assertThat(feeDown.status()).isEqualTo(503);
        assertThat(feeDown.code()).isEqualTo("DEPENDENCY_UNAVAILABLE");

        stubs.feeReply.set(DownstreamStubs.fee("SETTLED"));
        stubs.provisionReply.set(new DownstreamStubs.Reply(503, "{}"));
        assertThat(approve(id, APPROVE).status()).isEqualTo(503);
        assertThat(status(id)).isEqualTo("UNDER_REVIEW");
        assertThat(buildingsFor(id)).isZero();
    }

    @Test
    void approvalValidatesInputAndRole() throws Exception {
        String id = underReview();
        assertThat(approve(id, "{\"adminPhone\":\"01812345678\"}").status()).isEqualTo(400);
        assertThat(approve(id, "{\"adminPhone\":\"123\",\"reason\":\"ok\"}").status()).isEqualTo(400);
        assertThat(send("POST", "/api/v1/platform/building-applications/" + id + "/approve", token(applicant),
                APPROVE).status()).isEqualTo(403);
        assertThat(stubs.feeAuthorizations).isEmpty();

        String submittedOnly = send("POST", "/api/v1/building-applications", token(applicant),
                details("Solo", "Plot 1", "01711111111")).data().path("id").asString();
        send("POST", "/api/v1/building-applications/" + submittedOnly + "/submit", token(applicant), null);
        assertThat(approve(submittedOnly, APPROVE).code()).isEqualTo("INVALID_TRANSITION");
        assertThat(stubs.feeAuthorizations).isEmpty();
    }

    @Test
    void concurrentApprovalsCreateOneBuilding() throws Exception {
        String id = underReview();
        int racers = 3;
        var barrier = new CyclicBarrier(racers);
        var pool = Executors.newFixedThreadPool(racers);
        try {
            var results = new ArrayList<Future<Integer>>();
            for (int i = 0; i < racers; i++) {
                String racer = token(UUID.randomUUID(), "SUPER_ADMIN");
                results.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return send("POST", "/api/v1/platform/building-applications/" + id + "/approve", racer, APPROVE)
                            .status();
                }));
            }
            var statuses = new ArrayList<Integer>();
            for (var result : results) {
                statuses.add(result.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsOnlyOnce(200).containsOnly(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(buildingsFor(id)).isEqualTo(1);
    }

    private void addUnit(String buildingId) {
        UUID floor = UUID.randomUUID();
        jdbc.update("INSERT INTO building_floor (id, building_id, label, normalized_label, kind, display_order, "
                + "created_at, updated_at) VALUES (?, ?::uuid, 'Ground', 'GROUND', 'GROUND', 0, now(), now())",
                floor, buildingId);
        jdbc.update("INSERT INTO building_unit (id, building_id, floor_id, number, normalized_number, unit_type, "
                + "area_sqft, created_at, updated_at) VALUES (?, ?::uuid, ?, 'G1', 'G1', 'FLAT', 900, now(), now())",
                UUID.randomUUID(), buildingId, floor);
    }

    @Test
    void buildingLifecycleNeedsReasonsAndFollowsTheStateMachine() throws Exception {
        String buildingId = approve(underReview(), APPROVE).data().path("buildingId").asString();
        String base = "/api/v1/platform/buildings/" + buildingId;
        assertThat(send("POST", base + "/suspend", reviewerToken(), "{\"reason\":\"x\"}").code())
                .isEqualTo("INVALID_TRANSITION");
        assertThat(send("POST", base + "/activate", reviewerToken(), "{}").status()).isEqualTo(400);
        assertThat(send("POST", base + "/activate", token(applicant), "{\"reason\":\"go\"}").status()).isEqualTo(403);

        assertThat(send("POST", base + "/activate", reviewerToken(), "{\"reason\":\"No units yet\"}").code())
                .isEqualTo("NO_BUILDING_UNIT");
        addUnit(buildingId);
        assertThat(send("POST", base + "/activate", reviewerToken(), "{\"reason\":\"Admin confirmed\"}")
                .data().path("status").asString()).isEqualTo("ACTIVE");
        assertThat(send("POST", base + "/suspend", reviewerToken(), "{\"reason\":\"Payment dispute\"}")
                .data().path("status").asString()).isEqualTo("SUSPENDED");
        assertThat(send("POST", base + "/reactivate", reviewerToken(), "{\"reason\":\"Resolved\"}")
                .data().path("status").asString()).isEqualTo("ACTIVE");
        assertThat(send("POST", base + "/activate", reviewerToken(), "{\"reason\":\"again\"}").status()).isEqualTo(409);

        var reasons = jdbc.queryForList("SELECT to_status || ':' || reason FROM lifecycle_transition "
                + "WHERE entity_type = 'BUILDING' AND entity_id = ?::uuid ORDER BY occurred_at, id", String.class,
                buildingId);
        assertThat(reasons).containsExactly("ONBOARDING:Deed verified", "ACTIVE:Admin confirmed",
                "SUSPENDED:Payment dispute", "ACTIVE:Resolved");
        assertThat(send("GET", "/api/v1/platform/buildings/" + UUID.randomUUID(), reviewerToken(), null).code())
                .isEqualTo("BUILDING_NOT_FOUND");
    }

    @Test
    void duplicateSignalsFlagOpenApplicationsAndBuildings() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String existing = underReview("Lake " + unique + " Tower", "Plot " + unique, "01933000001");
        String phone = "0172200" + String.format("%04d", Math.abs(unique.hashCode()) % 10000);
        String first = underReview("Lake " + unique + " Tower", "House 1", phone);
        String buildingId = approve(first, APPROVE).data().path("buildingId").asString();
        String subject = underReview("LAKE-" + unique, "Plot " + unique, phone);

        var signals = send("GET", "/api/v1/platform/building-applications/" + subject + "/duplicates",
                reviewerToken(), null);
        assertThat(signals.status()).isEqualTo(200);
        var byId = new java.util.HashMap<String, List<String>>();
        signals.data().forEach(m -> {
            var on = new ArrayList<String>();
            m.path("matchedOn").forEach(s -> on.add(s.asString()));
            byId.put(m.path("id").asString(), on);
        });
        assertThat(byId.get(existing)).contains("NAME", "ADDRESS");
        assertThat(byId.get(buildingId)).contains("NAME", "CONTACT_PHONE");
        assertThat(byId).doesNotContainKey(subject).doesNotContainKey(first);
        assertThat(status(subject)).isEqualTo("UNDER_REVIEW");

        assertThat(send("GET", "/api/v1/platform/building-applications/" + subject + "/duplicates", token(applicant),
                null).status()).isEqualTo(403);
    }
}
