package com.buildingos.backoffice.onboarding;

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

/** F6-T3 (BOC-06, D-33): assisted onboarding session API against Postgres with stubbed auth/building services. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OnboardingSessionApiIntegrationTest {
    private static final String AUDIENCE = "backoffice-platform";
    private static final String BASE = "/api/v1/platform/backoffice/onboarding-sessions";
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

    private UUID operator;
    private UUID agent;
    private UUID otherAgent;
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
        registry.add("buildingos.onboarding.max-duration", () -> "P30D");
    }

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM lifecycle_transition");
        jdbc.update("DELETE FROM assisted_onboarding_session");
        stubs.reset();
        operator = UUID.randomUUID();
        agent = UUID.randomUUID();
        otherAgent = UUID.randomUUID();
        building = UUID.randomUUID();
        stubs.users.put(agent, List.of("ONBOARDING_AGENT"));
        stubs.users.put(otherAgent, List.of("ONBOARDING_AGENT"));
        stubs.buildings.add(building);
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private String token(UUID user, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of(roles));
    }

    private String operatorToken() throws Exception { return token(operator, "PLATFORM_ADMIN"); }
    private String agentToken() throws Exception { return token(agent, "ONBOARDING_AGENT"); }
    private String otherAgentToken() throws Exception { return token(otherAgent, "ONBOARDING_AGENT"); }

    private Reply send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private static String createBody(UUID buildingId, UUID assignee, String scopes, String reason, Instant expiresAt) {
        return "{\"buildingId\":\"" + buildingId + "\",\"assignedAgentUserId\":\"" + assignee + "\",\"accessScope\":"
                + scopes + ",\"reason\":" + (reason == null ? "null" : "\"" + reason + "\"") + ",\"expiresAt\":"
                + (expiresAt == null ? "null" : "\"" + expiresAt + "\"") + "}";
    }

    private Reply create(String token, UUID assignee) throws Exception {
        return send("POST", BASE, token, createBody(building, assignee,
                "[\"ONBOARDING_MANAGE_FLOORS_UNITS\",\"ONBOARDING_SEND_INVITATIONS\"]", "Configure 60 units",
                Instant.now().plus(Duration.ofDays(7))));
    }

    private UUID createdId() throws Exception {
        var reply = create(operatorToken(), agent);
        assertThat(reply.status()).isEqualTo(201);
        return UUID.fromString(reply.data().path("id").asString());
    }

    private Reply act(UUID id, String action, String token) throws Exception {
        return send("POST", BASE + "/" + id + "/" + action, token, null);
    }

    private int sessionCount() {
        return jdbc.queryForObject("SELECT count(*) FROM assisted_onboarding_session", Integer.class);
    }

    private List<Map<String, Object>> transitions(UUID id) {
        return jdbc.queryForList("SELECT from_status, to_status, actor_user_id, reason, occurred_at "
                + "FROM lifecycle_transition WHERE entity_type = 'ASSISTED_ONBOARDING_SESSION' AND entity_id = ? "
                + "ORDER BY occurred_at, id", id);
    }

    @Test
    void operatorCreatesAssignedSessionWithRelayedTokenAndAuditRow() throws Exception {
        String token = operatorToken();
        var reply = create(token, agent);
        assertThat(reply.status()).isEqualTo(201);
        var data = reply.data();
        assertThat(data.path("status").asString()).isEqualTo("ASSIGNED");
        assertThat(data.path("buildingId").asString()).isEqualTo(building.toString());
        assertThat(data.path("assignedAgentUserId").asString()).isEqualTo(agent.toString());
        assertThat(data.path("accessScope").size()).isEqualTo(2);
        assertThat(data.path("startedAt").asString()).isNotBlank();
        assertThat(stubs.authAuthorizations).containsExactly("Bearer " + token);
        assertThat(stubs.buildingAuthorizations).containsExactly("Bearer " + token);
        var rows = transitions(UUID.fromString(data.path("id").asString()));
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("from_status")).isNull();
            assertThat(row.get("to_status")).isEqualTo("ASSIGNED");
            assertThat(row.get("actor_user_id")).isEqualTo(operator);
            assertThat(row.get("reason")).isEqualTo("Configure 60 units");
            assertThat(row.get("occurred_at")).isNotNull();
        });
    }

    @Test
    void superAdminMayCreateButAgentsAndOthersMayNot() throws Exception {
        assertThat(create(token(operator, "SUPER_ADMIN"), agent).status()).isEqualTo(201);
        assertThat(create(agentToken(), agent).status()).isEqualTo(403);
        assertThat(create(token(UUID.randomUUID()), agent).status()).isEqualTo(403);
    }

    @Test
    void invalidScopeReasonOrExpiryIs400() throws Exception {
        String token = operatorToken();
        Instant ok = Instant.now().plus(Duration.ofDays(1));
        for (String body : List.of(
                createBody(building, agent, "[]", "r", ok),
                createBody(building, agent, "[\"SUPPORT_VIEW_MEMBERS\"]", "r", ok),
                createBody(building, agent, "[\"ONBOARDING_VIEW_BUILDING\",\"ONBOARDING_VIEW_BUILDING\"]", "r", ok),
                createBody(building, agent, "[\"ONBOARDING_VIEW_BUILDING\"]", null, ok),
                createBody(building, agent, "[\"ONBOARDING_VIEW_BUILDING\"]", "  ", ok),
                createBody(building, agent, "[\"ONBOARDING_VIEW_BUILDING\"]", "r", Instant.now().minusSeconds(5)),
                createBody(building, agent, "[\"ONBOARDING_VIEW_BUILDING\"]", "r",
                        Instant.now().plus(Duration.ofDays(31))),
                createBody(building, agent, "[\"ONBOARDING_VIEW_BUILDING\"]", "r", null))) {
            var reply = send("POST", BASE, token, body);
            assertThat(reply.status()).as(body).isEqualTo(400);
            assertThat(reply.code()).isEqualTo("INVALID_REQUEST");
        }
        assertThat(sessionCount()).isZero();
        assertThat(stubs.authAuthorizations).isEmpty();
    }

    @Test
    void nonAgentAssigneeOrUnknownBuildingIs422() throws Exception {
        UUID plainUser = UUID.randomUUID();
        stubs.users.put(plainUser, List.of("PLATFORM_ADMIN"));
        var notAgent = create(operatorToken(), plainUser);
        assertThat(notAgent.status()).isEqualTo(422);
        assertThat(notAgent.code()).isEqualTo("ASSIGNEE_NOT_ONBOARDING_AGENT");
        var unknownUser = create(operatorToken(), UUID.randomUUID());
        assertThat(unknownUser.status()).isEqualTo(422);
        assertThat(unknownUser.code()).isEqualTo("ASSIGNEE_NOT_ONBOARDING_AGENT");
        stubs.buildings.clear();
        var unknownBuilding = create(operatorToken(), agent);
        assertThat(unknownBuilding.status()).isEqualTo(422);
        assertThat(unknownBuilding.code()).isEqualTo("BUILDING_NOT_FOUND");
        assertThat(sessionCount()).isZero();
    }

    @Test
    void dependencyDownIs503AndStoresNothing() throws Exception {
        stubs.authDown = true;
        var authDown = create(operatorToken(), agent);
        assertThat(authDown.status()).isEqualTo(503);
        assertThat(authDown.code()).isEqualTo("DEPENDENCY_UNAVAILABLE");
        stubs.authDown = false;
        stubs.buildingDown = true;
        assertThat(create(operatorToken(), agent).status()).isEqualTo(503);
        assertThat(sessionCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lifecycle_transition", Integer.class)).isZero();
    }

    @Test
    void agentOnlyTransitionsRejectOperatorsWith403AndOtherAgentsWith404() throws Exception {
        UUID id = createdId();
        var byOperator = act(id, "start-work", operatorToken());
        assertThat(byOperator.status()).isEqualTo(403);
        assertThat(act(id, "start-work", otherAgentToken()).status()).isEqualTo(404);
        assertThat(act(id, "start-work", token(UUID.randomUUID())).status()).isEqualTo(403);

        var started = act(id, "start-work", agentToken());
        assertThat(started.status()).isEqualTo(200);
        assertThat(started.data().path("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(act(id, "await-customer", operatorToken()).status()).isEqualTo(403);
        assertThat(act(id, "await-customer", otherAgentToken()).status()).isEqualTo(404);
        assertThat(act(id, "await-customer", agentToken()).data().path("status").asString())
                .isEqualTo("WAITING_FOR_CUSTOMER");
        assertThat(act(id, "await-customer", agentToken()).status()).isEqualTo(409);
        assertThat(act(id, "start-work", agentToken()).data().path("status").asString()).isEqualTo("IN_PROGRESS");

        var rows = transitions(id);
        assertThat(rows).extracting(row -> row.get("to_status"))
                .containsExactly("ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER", "IN_PROGRESS");
        assertThat(rows.subList(1, 4)).allSatisfy(row -> assertThat(row.get("actor_user_id")).isEqualTo(agent));
    }

    @Test
    void cancelIsOperatorOnlyAndCompleteIsAgentOrOperator() throws Exception {
        UUID first = createdId();
        assertThat(act(first, "cancel", agentToken()).status()).isEqualTo(403);
        assertThat(act(first, "cancel", otherAgentToken()).status()).isEqualTo(404);
        var cancelled = send("POST", BASE + "/" + first + "/cancel", operatorToken(), "{\"reason\":\"Duplicate\"}");
        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(cancelled.data().path("status").asString()).isEqualTo("CANCELLED");
        assertThat(last(transitions(first)).get("reason")).isEqualTo("Duplicate");
        assertThat(act(first, "complete", operatorToken()).status()).isEqualTo(409);

        UUID second = createdId();
        assertThat(act(second, "complete", otherAgentToken()).status()).isEqualTo(404);
        var byAgent = act(second, "complete", agentToken());
        assertThat(byAgent.status()).isEqualTo(200);
        assertThat(byAgent.data().path("status").asString()).isEqualTo("COMPLETED");
        assertThat(byAgent.data().path("completedAt").asString()).isNotBlank();
        var again = act(second, "start-work", agentToken());
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("SESSION_ENDED");

        UUID third = createdId();
        assertThat(act(third, "complete", operatorToken()).data().path("status").asString()).isEqualTo("COMPLETED");
        assertThat(last(transitions(third)).get("actor_user_id")).isEqualTo(operator);
    }

    @Test
    void visibilityOperatorsSeeAllAgentsOnlyOwnOthersForbidden() throws Exception {
        UUID mine = createdId();
        var otherReply = create(operatorToken(), otherAgent);
        UUID theirs = UUID.fromString(otherReply.data().path("id").asString());

        assertThat(send("GET", BASE + "/" + mine, operatorToken(), null).status()).isEqualTo(200);
        assertThat(send("GET", BASE + "/" + mine, agentToken(), null).status()).isEqualTo(200);
        assertThat(send("GET", BASE + "/" + theirs, agentToken(), null).status()).isEqualTo(404);
        assertThat(send("GET", BASE + "/" + UUID.randomUUID(), operatorToken(), null).status()).isEqualTo(404);
        assertThat(send("GET", BASE + "/" + mine, token(UUID.randomUUID(), "BUILDING_ADMIN"), null).status())
                .isEqualTo(403);

        var all = send("GET", BASE, operatorToken(), null);
        assertThat(all.status()).isEqualTo(200);
        assertThat(all.data().size()).isEqualTo(2);
        assertThat(all.body().path("meta").path("total").asLong()).isEqualTo(2);
        var own = send("GET", BASE, agentToken(), null);
        assertThat(own.data().size()).isEqualTo(1);
        assertThat(own.data().get(0).path("id").asString()).isEqualTo(mine.toString());
        assertThat(send("GET", BASE + "?agentUserId=" + otherAgent, agentToken(), null).data().size()).isZero();
        assertThat(send("GET", BASE + "?agentUserId=" + otherAgent, operatorToken(), null).data().size()).isEqualTo(1);
        assertThat(send("GET", BASE + "?buildingId=" + building + "&status=ASSIGNED&page=0&size=1", operatorToken(),
                null).data().size()).isEqualTo(1);
        assertThat(send("GET", BASE + "?status=NOPE", operatorToken(), null).status()).isEqualTo(400);
        assertThat(send("GET", BASE + "?size=101", operatorToken(), null).status()).isEqualTo(400);
        assertThat(send("GET", BASE, token(UUID.randomUUID()), null).status()).isEqualTo(403);
    }

    @Test
    void expiredSessionsReportExpiredWithSystemTransitionAndReject409() throws Exception {
        UUID detailId = seedPastDue();
        var detail = send("GET", BASE + "/" + detailId, operatorToken(), null);
        assertThat(detail.status()).isEqualTo(200);
        assertThat(detail.data().path("status").asString()).isEqualTo("EXPIRED");
        var rows = transitions(detailId);
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("from_status")).isEqualTo("IN_PROGRESS");
            assertThat(row.get("to_status")).isEqualTo("EXPIRED");
            assertThat(row.get("actor_user_id")).isNull();
        });
        for (String action : List.of("start-work", "await-customer", "complete")) {
            var reply = act(detailId, action, agentToken());
            assertThat(reply.status()).as(action).isEqualTo(409);
            assertThat(reply.code()).isEqualTo("SESSION_ENDED");
        }
        assertThat(act(detailId, "cancel", operatorToken()).status()).isEqualTo(409);
        assertThat(transitions(detailId)).hasSize(1);

        UUID changeId = seedPastDue();
        assertThat(act(changeId, "complete", agentToken()).status()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT status FROM assisted_onboarding_session WHERE id = ?", String.class,
                changeId)).isEqualTo("EXPIRED");

        UUID listId = seedPastDue();
        var expired = send("GET", BASE + "?status=EXPIRED", operatorToken(), null);
        assertThat(expired.data().size()).isEqualTo(3);
        assertThat(transitions(listId)).singleElement()
                .satisfies(row -> assertThat(row.get("actor_user_id")).isNull());
    }

    private static <T> T last(List<T> list) { return list.get(list.size() - 1); }

    private UUID seedPastDue() {
        UUID id = UUID.randomUUID();
        Instant started = Instant.now().minus(Duration.ofDays(2));
        jdbc.update("INSERT INTO assisted_onboarding_session (id, building_id, assigned_agent_user_id, status, "
                        + "access_scope, reason, started_at, expires_at) VALUES (?, ?, ?, 'IN_PROGRESS', "
                        + "'{ONBOARDING_VIEW_BUILDING}'::text[], 'r', ?, ?)",
                id, building, agent, Timestamp.from(started), Timestamp.from(started.plus(Duration.ofDays(1))));
        return id;
    }
}
