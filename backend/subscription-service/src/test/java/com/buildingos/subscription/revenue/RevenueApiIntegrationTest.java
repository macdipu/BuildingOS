package com.buildingos.subscription.revenue;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.subscription.support.JwtFixtures;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class RevenueApiIntegrationTest {
    private static final String AUDIENCE = "subscription-platform";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("subscription_db").withUsername("subscription_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID admin = UUID.randomUUID();

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

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private String token(UUID user, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of(roles));
    }

    private String adminToken() throws Exception {
        return token(admin, "SUBSCRIPTION_ADMIN");
    }

    private Reply send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private static String planJson(String code, boolean selfService, String entitlements) {
        return "{\"code\":\"" + code + "\",\"name\":\"Plan " + code + "\",\"billingCycles\":[\"MONTHLY\",\"YEARLY\"],"
                + "\"selfService\":" + selfService + ",\"entitlements\":" + entitlements + "}";
    }

    private UUID createPlan(boolean selfService, String entitlements) throws Exception {
        String code = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        var reply = send("POST", "/api/v1/platform/subscription-plans", adminToken(), planJson(code, selfService, entitlements));
        assertThat(reply.status()).isEqualTo(201);
        return UUID.fromString(reply.data().path("id").asString());
    }

    private static String subscribeJson(UUID planId, String cycle) {
        return "{\"planId\":\"" + planId + "\",\"billingCycle\":\"" + cycle + "\"}";
    }

    private int auditCount(String action, String entityId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action = ? AND entity_id = ?",
                Integer.class, action, entityId);
    }

    @Test
    void platformEndpointsNeedTokenAndRevenueAdminRole() throws Exception {
        assertThat(send("GET", "/api/v1/platform/subscription-plans", null, null).status()).isEqualTo(401);
        var plainUser = token(UUID.randomUUID());
        assertThat(send("GET", "/api/v1/platform/subscription-plans", plainUser, null).status()).isEqualTo(403);
        var badBody = send("POST", "/api/v1/platform/subscription-plans", plainUser, "{\"nonsense\":true}");
        assertThat(badBody.status()).as("role check comes before body validation").isEqualTo(403);
        for (String role : List.of("SUPER_ADMIN", "PLATFORM_ADMIN", "SUBSCRIPTION_ADMIN")) {
            assertThat(send("GET", "/api/v1/platform/subscription-plans", token(UUID.randomUUID(), role), null).status())
                    .isEqualTo(200);
        }
        assertThat(send("GET", "/api/v1/platform/free-tier", token(UUID.randomUUID(), "SUPPORT_AGENT"), null).status())
                .isEqualTo(403);
    }

    @Test
    void planCrudValidationAndAudit() throws Exception {
        var planId = createPlan(false, "{\"rent_management.enabled\":true,\"max_units\":50}");
        assertThat(auditCount("PLAN_CREATED", planId.toString())).isEqualTo(1);

        var got = send("GET", "/api/v1/platform/subscription-plans/" + planId, adminToken(), null);
        assertThat(got.status()).isEqualTo(200);
        assertThat(got.data().path("status").asString()).isEqualTo("ACTIVE");
        assertThat(got.data().path("entitlements").path("max_units").asLong()).isEqualTo(50);
        String code = got.data().path("code").asString();

        var duplicate = send("POST", "/api/v1/platform/subscription-plans", adminToken(), planJson(code, false, "{}"));
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.code()).isEqualTo("PLAN_CODE_TAKEN");

        for (String bad : List.of(planJson("OK_CODE_1", false, "{\"unknown.key\":true}"),
                planJson("OK_CODE_2", false, "{\"max_units\":-3}"), planJson("bad code", false, "{}"),
                "{\"code\":\"OK_CODE_3\",\"name\":\"x\",\"billingCycles\":[],\"selfService\":false,\"entitlements\":{}}",
                "{\"code\":\"OK_CODE_4\",\"name\":\"x\",\"billingCycles\":[\"WEEKLY\"],\"selfService\":false,\"entitlements\":{}}",
                "{not json")) {
            var reply = send("POST", "/api/v1/platform/subscription-plans", adminToken(), bad);
            assertThat(reply.status()).as(bad).isEqualTo(400);
            assertThat(reply.code()).isEqualTo("INVALID_REQUEST");
        }

        var missing = send("GET", "/api/v1/platform/subscription-plans/" + UUID.randomUUID(), adminToken(), null);
        assertThat(missing.status()).isEqualTo(404);
        assertThat(missing.code()).isEqualTo("PLAN_NOT_FOUND");

        var list = send("GET", "/api/v1/platform/subscription-plans", adminToken(), null);
        assertThat(list.data().valueStream().map(node -> node.path("id").asString())).contains(planId.toString());
    }

    @Test
    void planEditsApplyImmediatelyToSubscribersAndRetireIsOneWay() throws Exception {
        var planId = createPlan(false, "{\"rent_management.enabled\":true}");
        var user = UUID.randomUUID();
        var granted = send("POST", "/api/v1/platform/users/" + user + "/subscription", adminToken(),
                subscribeJson(planId, "MONTHLY"));
        assertThat(granted.status()).isEqualTo(201);
        assertThat(granted.data().path("grantedBy").asString()).isEqualTo("ADMIN");

        var edit = "{\"name\":\"Edited\",\"billingCycles\":[\"YEARLY\"],\"selfService\":false,"
                + "\"entitlements\":{\"work_orders.enabled\":true}}";
        assertThat(send("PUT", "/api/v1/platform/subscription-plans/" + planId, adminToken(), edit).status()).isEqualTo(200);
        assertThat(auditCount("PLAN_UPDATED", planId.toString())).isEqualTo(1);

        var mine = send("GET", "/api/v1/me/entitlements", token(user), null).data();
        assertThat(mine.path("work_orders.enabled").asBoolean()).isTrue();
        assertThat(mine.has("rent_management.enabled")).as("edit replaced plan features at once").isFalse();
        assertThat(mine.path("maintenance.enabled").asBoolean()).isTrue();

        var retired = send("POST", "/api/v1/platform/subscription-plans/" + planId + "/retire", adminToken(), null);
        assertThat(retired.data().path("status").asString()).isEqualTo("RETIRED");
        assertThat(send("PUT", "/api/v1/platform/subscription-plans/" + planId, adminToken(), edit).code())
                .isEqualTo("PLAN_RETIRED");
        var lateGrant = send("POST", "/api/v1/platform/users/" + UUID.randomUUID() + "/subscription", adminToken(),
                subscribeJson(planId, "YEARLY"));
        assertThat(lateGrant.status()).isEqualTo(409);
        assertThat(lateGrant.code()).isEqualTo("PLAN_RETIRED");
        assertThat(send("GET", "/api/v1/me/entitlements", token(user), null).data().path("work_orders.enabled").asBoolean())
                .as("existing subscription keeps its retired plan").isTrue();
    }

    @Test
    void entitlementsArePerUserOnly() throws Exception {
        var planId = createPlan(false, "{\"rent_management.enabled\":true,\"reports.pdf_export\":true}");
        var owner = UUID.randomUUID();
        var tenant = UUID.randomUUID();
        send("POST", "/api/v1/platform/users/" + owner + "/subscription", adminToken(), subscribeJson(planId, "MONTHLY"));

        var ownerView = send("GET", "/api/v1/me/entitlements", token(owner), null).data();
        assertThat(ownerView.path("rent_management.enabled").asBoolean()).isTrue();
        assertThat(ownerView.path("maintenance.enabled").asBoolean()).isTrue();

        var tenantView = send("GET", "/api/v1/me/entitlements", token(tenant), null).data();
        assertThat(tenantView.path("maintenance.enabled").asBoolean()).isTrue();
        assertThat(tenantView.has("rent_management.enabled")).isFalse();

        var adminView = send("GET", "/api/v1/platform/users/" + owner + "/subscription", adminToken(), null);
        assertThat(adminView.data().path("planId").asString()).isEqualTo(planId.toString());
        var none = send("GET", "/api/v1/platform/users/" + tenant + "/subscription", adminToken(), null);
        assertThat(none.status()).isEqualTo(404);
        assertThat(none.code()).isEqualTo("SUBSCRIPTION_NOT_FOUND");
    }

    @Test
    void selfSubscribeRules() throws Exception {
        var adminOnlyPlan = createPlan(false, "{\"offline_sync.enabled\":true}");
        var openPlan = createPlan(true, "{\"offline_sync.enabled\":true}");
        var user = UUID.randomUUID();

        var listed = send("GET", "/api/v1/me/plans", token(user), null).data();
        var ids = listed.valueStream().map(node -> node.path("id").asString()).toList();
        assertThat(ids).contains(openPlan.toString()).doesNotContain(adminOnlyPlan.toString());

        var refused = send("POST", "/api/v1/me/subscription", token(user), subscribeJson(adminOnlyPlan, "MONTHLY"));
        assertThat(refused.status()).isEqualTo(403);
        assertThat(refused.code()).isEqualTo("PLAN_NOT_SELF_SERVICE");

        var wrongCycle = send("POST", "/api/v1/me/subscription", token(user), subscribeJson(openPlan, "QUARTERLY"));
        assertThat(wrongCycle.status()).isEqualTo(409);
        assertThat(wrongCycle.code()).isEqualTo("BILLING_CYCLE_NOT_OFFERED");

        var ok = send("POST", "/api/v1/me/subscription", token(user), subscribeJson(openPlan, "YEARLY"));
        assertThat(ok.status()).isEqualTo(201);
        assertThat(ok.data().path("grantedBy").asString()).isEqualTo("SELF_SERVICE");
        assertThat(ok.data().path("userId").asString()).isEqualTo(user.toString());
        assertThat(ok.data().path("effectiveEntitlements").path("offline_sync.enabled").asBoolean()).isTrue();

        var again = send("POST", "/api/v1/me/subscription", token(user), subscribeJson(openPlan, "YEARLY"));
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("ALREADY_SUBSCRIBED");
        var adminAgain = send("POST", "/api/v1/platform/users/" + user + "/subscription", adminToken(),
                subscribeJson(adminOnlyPlan, "MONTHLY"));
        assertThat(adminAgain.code()).isEqualTo("ALREADY_SUBSCRIBED");
    }

    @Test
    void concurrentSubscribeHasExactlyOneWinner() throws Exception {
        var planId = createPlan(true, "{}");
        var user = UUID.randomUUID();
        var userToken = token(user);
        var pool = Executors.newFixedThreadPool(6);
        var barrier = new CyclicBarrier(6);
        try {
            var futures = new ArrayList<Future<Integer>>();
            for (int i = 0; i < 6; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return send("POST", "/api/v1/me/subscription", userToken, subscribeJson(planId, "MONTHLY")).status();
                }));
            }
            var statuses = new ArrayList<Integer>();
            for (var future : futures) {
                statuses.add(future.get(20, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsOnlyOnce(201).containsOnly(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM subscription WHERE subject_id = ?", Integer.class, user))
                .isEqualTo(1);
    }

    @Test
    void freeTierIsDataAndEditsApplyImmediately() throws Exception {
        var user = UUID.randomUUID();
        var initial = send("GET", "/api/v1/platform/free-tier", adminToken(), null);
        assertThat(initial.data().path("maintenance.enabled").asBoolean()).isTrue();
        try {
            var updated = send("PUT", "/api/v1/platform/free-tier", adminToken(),
                    "{\"maintenance.enabled\":true,\"work_orders.enabled\":true}");
            assertThat(updated.status()).isEqualTo(200);
            assertThat(send("GET", "/api/v1/me/entitlements", token(user), null).data().path("work_orders.enabled")
                    .asBoolean()).isTrue();
            assertThat(send("PUT", "/api/v1/platform/free-tier", adminToken(), "{\"bogus\":1}").status()).isEqualTo(400);
            assertThat(auditCount("FREE_TIER_UPDATED", "1")).isGreaterThanOrEqualTo(1);
        } finally {
            send("PUT", "/api/v1/platform/free-tier", adminToken(), "{\"maintenance.enabled\":true}");
        }
    }

    @Test
    void buildingCreationFeeFlow() throws Exception {
        var application = UUID.randomUUID();
        String status = "/api/v1/platform/fees/BUILDING_CREATION/status?referenceType=BUILDING_APPLICATION&referenceId="
                + application;
        String payments = "/api/v1/platform/fees/BUILDING_CREATION/payments";
        String schedule = "/api/v1/platform/fees/BUILDING_CREATION";
        jdbc.update("DELETE FROM payment_record");
        jdbc.update("DELETE FROM fee_schedule");

        var unconfigured = send("GET", status, adminToken(), null);
        assertThat(unconfigured.status()).isEqualTo(409);
        assertThat(unconfigured.code()).isEqualTo("FEE_NOT_CONFIGURED");
        assertThat(send("POST", payments, adminToken(), payment(application, "100", "BDT")).code())
                .isEqualTo("FEE_NOT_CONFIGURED");

        assertThat(send("PUT", schedule, adminToken(), "{\"amount\":5000.00,\"currency\":\"BDT\",\"required\":true}")
                .status()).isEqualTo(200);
        assertThat(send("GET", status, adminToken(), null).data().path("status").asString()).isEqualTo("UNPAID");

        assertThat(send("POST", payments, adminToken(), payment(application, "3000", "BDT")).status()).isEqualTo(201);
        assertThat(send("GET", status, adminToken(), null).data().path("status").asString()).isEqualTo("UNPAID");
        var mismatch = send("POST", payments, adminToken(), payment(application, "2000", "USD"));
        assertThat(mismatch.status()).isEqualTo(409);
        assertThat(mismatch.code()).isEqualTo("CURRENCY_MISMATCH");
        assertThat(send("POST", payments, adminToken(), payment(application, "2000", "BDT")).status()).isEqualTo(201);

        var settled = send("GET", status, adminToken(), null).data();
        assertThat(settled.path("status").asString()).isEqualTo("SETTLED");
        assertThat(settled.path("payments").size()).isEqualTo(2);
        assertThat(settled.path("payments").get(0).path("method").asString()).isEqualTo("MANUAL");

        var other = "/api/v1/platform/fees/BUILDING_CREATION/status?referenceType=BUILDING_APPLICATION&referenceId="
                + UUID.randomUUID();
        assertThat(send("GET", other, adminToken(), null).data().path("status").asString()).isEqualTo("UNPAID");
        send("PUT", schedule, adminToken(), "{\"amount\":5000.00,\"currency\":\"BDT\",\"required\":false}");
        assertThat(send("GET", other, adminToken(), null).data().path("status").asString()).isEqualTo("NOT_REQUIRED");

        assertThat(auditCount("FEE_SCHEDULE_UPDATED", "BUILDING_CREATION")).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action = 'PAYMENT_RECORDED'", Integer.class))
                .isEqualTo(2);
        assertThat(send("GET", "/api/v1/platform/fees/NOT_A_FEE", adminToken(), null).status()).isEqualTo(400);
        assertThat(send("PUT", schedule, adminToken(), "{\"amount\":-1,\"currency\":\"BDT\",\"required\":true}").status())
                .isEqualTo(400);
    }

    private static String payment(UUID application, String amount, String currency) {
        return "{\"referenceType\":\"BUILDING_APPLICATION\",\"referenceId\":\"" + application + "\",\"amount\":" + amount
                + ",\"currency\":\"" + currency + "\",\"externalReference\":\"bank-slip-42\",\"paidOn\":\"2026-09-23\"}";
    }
}
