package com.buildingos.backoffice.supportsession;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.backoffice.support.DownstreamStubs;
import com.buildingos.backoffice.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

/** F6-T4 (BOC-07, D-36): support sessions and elevated approvals against Postgres with stubbed auth/building. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class SupportSessionApiIntegrationTest {
    private static final String AUDIENCE = "backoffice-platform";
    private static final String SESSIONS = "/api/v1/platform/backoffice/support-sessions";
    private static final String APPROVALS = "/api/v1/platform/backoffice/elevated-approvals";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;
    private static DownstreamStubs stubs;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("back_office_db").withUsername("back_office_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID agent;
    private UUID otherAgent;
    private UUID superAdmin;
    private UUID otherSuperAdmin;
    private UUID platformAdmin;
    private UUID customer;
    private UUID building;

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
        registry.add("buildingos.services.building-url", stubs::url);
        registry.add("buildingos.support.max-duration", () -> "PT8H");
    }

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM lifecycle_transition");
        jdbc.update("DELETE FROM elevated_approval_request");
        jdbc.update("DELETE FROM support_session");
        stubs.reset();
        agent = UUID.randomUUID();
        otherAgent = UUID.randomUUID();
        superAdmin = UUID.randomUUID();
        otherSuperAdmin = UUID.randomUUID();
        platformAdmin = UUID.randomUUID();
        customer = UUID.randomUUID();
        building = UUID.randomUUID();
        stubs.users.put(customer, List.of());
        stubs.buildings.add(building);
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private String token(UUID user, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of(roles));
    }

    private String agentToken() throws Exception { return token(agent, "SUPPORT_AGENT"); }
    private String otherAgentToken() throws Exception { return token(otherAgent, "SUPPORT_AGENT"); }
    private String superToken() throws Exception { return token(superAdmin, "SUPER_ADMIN"); }
    private String otherSuperToken() throws Exception { return token(otherSuperAdmin, "SUPER_ADMIN"); }
    private String platformAdminToken() throws Exception { return token(platformAdmin, "PLATFORM_ADMIN"); }

    private Reply send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private static String quoted(Object value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String startBody(UUID targetUser, UUID buildingId, String scopes, String reason,
            Instant expiresAt) {
        return "{\"targetUserId\":" + quoted(targetUser) + ",\"buildingId\":" + quoted(buildingId)
                + ",\"permissionScope\":" + scopes + ",\"reason\":" + quoted(reason) + ",\"expiresAt\":"
                + quoted(expiresAt) + "}";
    }

    private Reply start(String token, String scopes) throws Exception {
        return send("POST", SESSIONS, token, startBody(customer, building, scopes, "Ticket 42: cannot see units",
                Instant.now().plus(Duration.ofHours(2))));
    }

    private UUID startedId(String token, String scopes) throws Exception {
        var reply = start(token, scopes);
        assertThat(reply.status()).as(reply.body().toString()).isEqualTo(201);
        return UUID.fromString(reply.data().path("id").asString());
    }

    private String scopeCheck(UUID sessionId, String scope, String token) throws Exception {
        var reply = send("GET", SESSIONS + "/" + sessionId + "/scope-check?scope=" + scope, token, null);
        assertThat(reply.status()).as(reply.body().toString()).isEqualTo(200);
        return reply.data().path("result").asString();
    }

    private List<Map<String, Object>> transitions(String type, UUID id) {
        return jdbc.queryForList("SELECT from_status, to_status, actor_user_id, reason FROM lifecycle_transition "
                + "WHERE entity_type = ? AND entity_id = ? ORDER BY occurred_at, id", type, id);
    }

    @Test
    void agentStartsSessionWithRelayedTokenAndAuditRow() throws Exception {
        String token = agentToken();
        var reply = start(token, "[\"SUPPORT_VIEW_UNITS\",\"SUPPORT_VIEW_MEMBERS\"]");
        assertThat(reply.status()).isEqualTo(201);
        var data = reply.data();
        assertThat(data.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(data.path("platformUserId").asString()).isEqualTo(agent.toString());
        assertThat(data.path("targetUserId").asString()).isEqualTo(customer.toString());
        assertThat(data.path("permissionScope").size()).isEqualTo(2);
        assertThat(data.path("elevatedApprovals").size()).isZero();
        assertThat(stubs.authAuthorizations).containsExactly("Bearer " + token);
        assertThat(stubs.buildingAuthorizations).containsExactly("Bearer " + token);
        UUID id = UUID.fromString(data.path("id").asString());
        assertThat(transitions("SUPPORT_SESSION", id)).singleElement().satisfies(row -> {
            assertThat(row.get("from_status")).isNull();
            assertThat(row.get("to_status")).isEqualTo("ACTIVE");
            assertThat(row.get("actor_user_id")).isEqualTo(agent);
            assertThat(row.get("reason")).isEqualTo("Ticket 42: cannot see units");
        });
        assertThat(scopeCheck(id, "SUPPORT_VIEW_UNITS", token)).isEqualTo("ALLOWED");
        assertThat(scopeCheck(id, "SUPPORT_EDIT_UNIT", token)).isEqualTo("NOT_GRANTED");
    }

    @Test
    void startValidationDependenciesAndRoles() throws Exception {
        Instant in2h = Instant.now().plus(Duration.ofHours(2));
        String scopes = "[\"SUPPORT_VIEW_UNITS\"]";
        for (String body : List.of(
                startBody(customer, building, "[\"NOT_A_SCOPE\"]", "r", in2h),
                startBody(customer, building, "[]", "r", in2h),
                startBody(customer, building, scopes, " ", in2h),
                startBody(customer, building, scopes, "r", Instant.now().minusSeconds(60)),
                startBody(customer, building, scopes, "r", Instant.now().plus(Duration.ofHours(9))),
                startBody(null, null, scopes, "r", in2h))) {
            var reply = send("POST", SESSIONS, agentToken(), body);
            assertThat(reply.status()).as(body).isEqualTo(400);
        }
        var unknownUser = send("POST", SESSIONS, agentToken(), startBody(UUID.randomUUID(), null, scopes, "r", in2h));
        assertThat(unknownUser.status()).isEqualTo(422);
        assertThat(unknownUser.code()).isEqualTo("TARGET_USER_NOT_FOUND");
        var unknownBuilding = send("POST", SESSIONS, agentToken(),
                startBody(null, UUID.randomUUID(), scopes, "r", in2h));
        assertThat(unknownBuilding.status()).isEqualTo(422);
        assertThat(unknownBuilding.code()).isEqualTo("BUILDING_NOT_FOUND");

        stubs.authDown = true;
        assertThat(start(agentToken(), scopes).status()).isEqualTo(503);
        stubs.authDown = false;
        stubs.buildingDown = true;
        assertThat(start(agentToken(), scopes).status()).isEqualTo(503);
        stubs.buildingDown = false;

        assertThat(start(token(UUID.randomUUID(), "ONBOARDING_AGENT"), scopes).status()).isEqualTo(403);
        assertThat(start(token(UUID.randomUUID()), scopes).status()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM support_session", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lifecycle_transition", Integer.class)).isZero();

        assertThat(start(platformAdminToken(), scopes).status()).isEqualTo(201);
        assertThat(start(superToken(), scopes).status()).isEqualTo(201);
    }

    @Test
    void highRiskScopeAtStartNeedsFourEyesApproval() throws Exception {
        var reply = start(superToken(), "[\"SUPPORT_VIEW_UNITS\",\"SUPPORT_TRANSFER_OWNERSHIP\"]");
        assertThat(reply.status()).isEqualTo(201);
        UUID id = UUID.fromString(reply.data().path("id").asString());
        var approvals = reply.data().path("elevatedApprovals");
        assertThat(approvals.size()).isEqualTo(1);
        assertThat(approvals.get(0).path("requestedScope").asString()).isEqualTo("SUPPORT_TRANSFER_OWNERSHIP");
        assertThat(approvals.get(0).path("status").asString()).isEqualTo("PENDING");
        UUID approvalId = UUID.fromString(approvals.get(0).path("id").asString());
        assertThat(transitions("ELEVATED_APPROVAL_REQUEST", approvalId)).singleElement()
                .satisfies(row -> assertThat(row.get("to_status")).isEqualTo("PENDING"));

        assertThat(scopeCheck(id, "SUPPORT_VIEW_UNITS", superToken())).isEqualTo("ALLOWED");
        assertThat(scopeCheck(id, "SUPPORT_TRANSFER_OWNERSHIP", superToken())).isEqualTo("PENDING_APPROVAL");

        var pendingList = send("GET", APPROVALS + "?status=PENDING", otherSuperToken(), null);
        assertThat(pendingList.data().size()).isEqualTo(1);
        assertThat(pendingList.body().path("meta").path("total").asInt()).isEqualTo(1);

        var self = send("POST", APPROVALS + "/" + approvalId + "/approve", superToken(), "{}");
        assertThat(self.status()).isEqualTo(403);
        var byPlatformAdmin = send("POST", APPROVALS + "/" + approvalId + "/approve", platformAdminToken(), "{}");
        assertThat(byPlatformAdmin.status()).isEqualTo(403);
        assertThat(send("POST", APPROVALS + "/" + UUID.randomUUID() + "/approve", otherSuperToken(), "{}").status())
                .isEqualTo(404);

        var approved = send("POST", APPROVALS + "/" + approvalId + "/approve", otherSuperToken(),
                "{\"reason\":\"Verified with owner\"}");
        assertThat(approved.status()).isEqualTo(200);
        assertThat(approved.data().path("status").asString()).isEqualTo("APPROVED");
        assertThat(approved.data().path("decidedBy").asString()).isEqualTo(otherSuperAdmin.toString());
        assertThat(scopeCheck(id, "SUPPORT_TRANSFER_OWNERSHIP", superToken())).isEqualTo("ALLOWED");
        assertThat(transitions("ELEVATED_APPROVAL_REQUEST", approvalId)).hasSize(2).last().satisfies(row -> {
            assertThat(row.get("from_status")).isEqualTo("PENDING");
            assertThat(row.get("to_status")).isEqualTo("APPROVED");
            assertThat(row.get("actor_user_id")).isEqualTo(otherSuperAdmin);
        });
        var again = send("POST", APPROVALS + "/" + approvalId + "/deny", otherSuperToken(), "{\"reason\":\"x\"}");
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("APPROVAL_NOT_PENDING");
    }

    @Test
    void ownerRequestsLaterDenialNeedsReasonAndDeniedScopeMayBeRequestedAgain() throws Exception {
        UUID id = startedId(agentToken(), "[\"SUPPORT_VIEW_PAYMENTS\"]");
        String path = SESSIONS + "/" + id + "/elevated-approvals";
        String body = "{\"scope\":\"SUPPORT_REVERSE_PAYMENT\",\"reason\":\"Duplicate charge\"}";
        assertThat(send("POST", path, agentToken(), "{\"scope\":\"SUPPORT_VIEW_UNITS\",\"reason\":\"r\"}").status())
                .isEqualTo(400);
        assertThat(send("POST", path, agentToken(), "{\"scope\":\"SUPPORT_REVERSE_PAYMENT\"}").status())
                .isEqualTo(400);
        assertThat(send("POST", path, otherAgentToken(), body).status()).isEqualTo(404);
        assertThat(send("POST", path, platformAdminToken(), body).status()).isEqualTo(403);

        var requested = send("POST", path, agentToken(), body);
        assertThat(requested.status()).isEqualTo(201);
        UUID approvalId = UUID.fromString(requested.data().path("id").asString());
        assertThat(transitions("ELEVATED_APPROVAL_REQUEST", approvalId)).singleElement().satisfies(row -> {
            assertThat(row.get("actor_user_id")).isEqualTo(agent);
            assertThat(row.get("reason")).isEqualTo("Duplicate charge");
        });
        var duplicate = send("POST", path, agentToken(), body);
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.code()).isEqualTo("APPROVAL_ALREADY_PENDING");
        assertThat(scopeCheck(id, "SUPPORT_REVERSE_PAYMENT", agentToken())).isEqualTo("PENDING_APPROVAL");

        assertThat(send("POST", APPROVALS + "/" + approvalId + "/deny", superToken(), "{}").status()).isEqualTo(400);
        var denied = send("POST", APPROVALS + "/" + approvalId + "/deny", superToken(), "{\"reason\":\"Refund first\"}");
        assertThat(denied.status()).isEqualTo(200);
        assertThat(denied.data().path("status").asString()).isEqualTo("DENIED");
        assertThat(denied.data().path("decisionReason").asString()).isEqualTo("Refund first");
        assertThat(scopeCheck(id, "SUPPORT_REVERSE_PAYMENT", agentToken())).isEqualTo("NOT_GRANTED");

        assertThat(send("POST", path, agentToken(), body).status()).isEqualTo(201);
        var detail = send("GET", SESSIONS + "/" + id, agentToken(), null);
        assertThat(detail.data().path("elevatedApprovals").size()).isEqualTo(2);
        assertThat(detail.data().path("permissionScope").size()).isEqualTo(2);

        var agentView = send("GET", APPROVALS, agentToken(), null);
        assertThat(agentView.data().size()).isEqualTo(2);
        assertThat(send("GET", APPROVALS, otherAgentToken(), null).data().size()).isZero();
        assertThat(send("GET", APPROVALS, token(UUID.randomUUID()), null).status()).isEqualTo(403);
    }

    @Test
    void visibilityAndEnd() throws Exception {
        UUID mine = startedId(agentToken(), "[\"SUPPORT_VIEW_UNITS\"]");
        startedId(otherAgentToken(), "[\"SUPPORT_VIEW_UNITS\"]");
        assertThat(send("GET", SESSIONS + "/" + mine, otherAgentToken(), null).status()).isEqualTo(404);
        assertThat(send("GET", SESSIONS + "/" + mine, token(UUID.randomUUID()), null).status()).isEqualTo(403);
        assertThat(send("GET", SESSIONS + "/" + mine, platformAdminToken(), null).status()).isEqualTo(200);
        assertThat(send("GET", SESSIONS, agentToken(), null).data().size()).isEqualTo(1);
        assertThat(send("GET", SESSIONS + "?platformUserId=" + otherAgent, agentToken(), null).data().size())
                .isZero();
        assertThat(send("GET", SESSIONS, superToken(), null).data().size()).isEqualTo(2);
        assertThat(send("GET", SESSIONS + "?platformUserId=" + agent + "&active=true", superToken(), null)
                .data().size()).isEqualTo(1);
        assertThat(send("GET", SESSIONS, token(UUID.randomUUID(), "ONBOARDING_AGENT"), null).status())
                .isEqualTo(403);

        assertThat(send("POST", SESSIONS + "/" + mine + "/end", otherAgentToken(), null).status()).isEqualTo(404);
        var ended = send("POST", SESSIONS + "/" + mine + "/end", agentToken(), "{\"reason\":\"Resolved\"}");
        assertThat(ended.status()).isEqualTo(200);
        assertThat(ended.data().path("status").asString()).isEqualTo("ENDED");
        assertThat(ended.data().path("endedAt").asString()).isNotBlank();
        var again = send("POST", SESSIONS + "/" + mine + "/end", platformAdminToken(), null);
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("SESSION_ENDED");
        assertThat(transitions("SUPPORT_SESSION", mine)).hasSize(2).last().satisfies(row -> {
            assertThat(row.get("to_status")).isEqualTo("ENDED");
            assertThat(row.get("actor_user_id")).isEqualTo(agent);
            assertThat(row.get("reason")).isEqualTo("Resolved");
        });
        assertThat(scopeCheck(mine, "SUPPORT_VIEW_UNITS", agentToken())).isEqualTo("SESSION_ENDED");
        assertThat(send("GET", SESSIONS + "?active=false", superToken(), null).data().size()).isEqualTo(1);

        UUID adminEnds = startedId(agentToken(), "[\"SUPPORT_VIEW_UNITS\"]");
        assertThat(send("POST", SESSIONS + "/" + adminEnds + "/end", platformAdminToken(), null).status())
                .isEqualTo(200);
    }

    @Test
    void expiredSessionReportsExpiredWithSystemTransitionAndRejectsChanges() throws Exception {
        UUID id = seedPastDue();
        UUID approvalId = UUID.randomUUID();
        jdbc.update("INSERT INTO elevated_approval_request (id, support_session_id, requested_scope, status, "
                + "requested_at) VALUES (?, ?, 'SUPPORT_TRANSFER_OWNERSHIP', 'PENDING', ?)", approvalId, id,
                Timestamp.from(Instant.now().minus(Duration.ofHours(3))));

        var detail = send("GET", SESSIONS + "/" + id, agentToken(), null);
        assertThat(detail.status()).isEqualTo(200);
        assertThat(detail.data().path("status").asString()).isEqualTo("EXPIRED");
        assertThat(transitions("SUPPORT_SESSION", id)).singleElement().satisfies(row -> {
            assertThat(row.get("from_status")).isEqualTo("ACTIVE");
            assertThat(row.get("to_status")).isEqualTo("EXPIRED");
            assertThat(row.get("actor_user_id")).isNull();
        });
        assertThat(scopeCheck(id, "SUPPORT_TRANSFER_OWNERSHIP", agentToken())).isEqualTo("SESSION_ENDED");
        assertThat(send("POST", SESSIONS + "/" + id + "/end", agentToken(), null).status()).isEqualTo(409);
        assertThat(send("POST", SESSIONS + "/" + id + "/elevated-approvals", agentToken(),
                "{\"scope\":\"SUPPORT_REVERSE_PAYMENT\",\"reason\":\"r\"}").status()).isEqualTo(409);
        var approve = send("POST", APPROVALS + "/" + approvalId + "/approve", superToken(), "{}");
        assertThat(approve.status()).isEqualTo(409);
        assertThat(approve.code()).isEqualTo("SESSION_ENDED");
        assertThat(transitions("SUPPORT_SESSION", id)).hasSize(1);
        assertThat(transitions("ELEVATED_APPROVAL_REQUEST", approvalId)).isEmpty();

        UUID listed = seedPastDue();
        assertThat(send("GET", SESSIONS + "?active=false", superToken(), null).data().size()).isEqualTo(2);
        assertThat(transitions("SUPPORT_SESSION", listed)).singleElement()
                .satisfies(row -> assertThat(row.get("actor_user_id")).isNull());
    }

    private UUID seedPastDue() {
        UUID id = UUID.randomUUID();
        Instant started = Instant.now().minus(Duration.ofHours(5));
        jdbc.update("INSERT INTO support_session (id, platform_user_id, target_user_id, reason, permission_scope, "
                        + "started_at, expires_at) VALUES (?, ?, ?, 'r', '{SUPPORT_VIEW_UNITS}'::text[], ?, ?)",
                id, agent, customer, Timestamp.from(started), Timestamp.from(started.plus(Duration.ofHours(1))));
        return id;
    }
}
