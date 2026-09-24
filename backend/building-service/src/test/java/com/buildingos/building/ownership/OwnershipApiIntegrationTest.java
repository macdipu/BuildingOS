package com.buildingos.building.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/** F4-T4a: assignment, partial/full transfer, history visibility, idempotency, outbox atomicity. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OwnershipApiIntegrationTest {
    private static final String AUDIENCE = "building-platform";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID building;
    private UUID unit;
    private UUID admin;
    private final Map<String, UUID> owners = new HashMap<>();
    private final String today = LocalDate.now(ZoneId.of("Asia/Dhaka")).toString();

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

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        UUID application = UUID.randomUUID();
        building = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(), Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) VALUES (?, ?, 'Rose Tower', 'RESIDENTIAL', "
                + "'Road 1', 'Mirpur', 'Dhaka', '01712345678', 'ACTIVE', 0, ?, ?)",
                building, application, Timestamp.from(now), Timestamp.from(now));
        admin = UUID.randomUUID();
        member(admin, "BUILDING_ADMIN");
        UUID floor = UUID.randomUUID();
        jdbc.update("INSERT INTO building_floor (id, building_id, label, normalized_label, kind, display_order, "
                + "created_at, updated_at) VALUES (?, ?, '4th', '4TH', 'REGULAR', 4, now(), now())", floor, building);
        unit = UUID.randomUUID();
        jdbc.update("INSERT INTO building_unit (id, building_id, floor_id, number, normalized_number, unit_type, "
                + "area_sqft, created_at, updated_at) VALUES (?, ?, ?, '4B', '4B', 'FLAT', 1200, now(), now())",
                unit, building, floor);
        for (String name : List.of("A", "B", "C", "D")) {
            owners.put(name, UUID.randomUUID());
            member(owners.get(name), "OWNER");
        }
    }

    private void member(UUID user, String role) {
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE', now())", UUID.randomUUID(), building, user, role);
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private Reply send(String method, String path, UUID user, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/buildings/" + building
                        + "/units/" + unit + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + fixtures.userToken(AUDIENCE, user, List.of()));
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private Reply assign(String owner, String share, long version, UUID operation) throws Exception {
        return send("POST", "/ownerships", admin, "{\"ownerUserId\":\"" + owners.get(owner) + "\",\"share\":" + share
                + ",\"effectiveDate\":\"" + today + "\",\"expectedVersion\":" + version + ",\"operationId\":\""
                + operation + "\"}");
    }

    private String transferBody(String from, String to, String share, long version, UUID operation) {
        return "{\"sourceOwnerUserId\":\"" + owners.get(from) + "\",\"recipientUserId\":\"" + owners.get(to)
                + "\",\"share\":" + share + ",\"effectiveDate\":\"" + today + "\",\"reason\":\"Sale\","
                + "\"reference\":\"Deed 12\",\"expectedVersion\":" + version + ",\"operationId\":\"" + operation + "\"}";
    }

    private Reply transfer(String from, String to, String share, long version, UUID operation) throws Exception {
        return send("POST", "/ownership-transfers", admin, transferBody(from, to, share, version, operation));
    }

    private Map<String, String> current(JsonNode data) {
        Map<String, String> byName = new HashMap<>();
        data.path("current").forEach(p -> owners.forEach((name, id) -> {
            if (id.toString().equals(p.path("ownerUserId").asString())) {
                byName.put(name, p.path("share").decimalValue().stripTrailingZeros().toPlainString());
            }
        }));
        return byName;
    }

    private int rows(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE building_id = ?", Integer.class, building);
    }

    @Test
    void assignAndTransferFollowTheAcceptanceExample() throws Exception {
        assertThat(assign("A", "60", 0, UUID.randomUUID()).status()).isEqualTo(201);
        var both = assign("B", "40", 1, UUID.randomUUID());
        assertThat(current(both.data())).containsExactlyInAnyOrderEntriesOf(Map.of("A", "60", "B", "40"));
        assertThat(assign("C", "0.0001", 2, UUID.randomUUID()).code()).isEqualTo("SHARE_EXCEEDED");
        assertThat(assign("C", "1", 1, UUID.randomUUID()).code()).isEqualTo("STALE_VERSION");

        var moved = transfer("A", "C", "20", 2, UUID.randomUUID());
        assertThat(moved.status()).isEqualTo(201);
        assertThat(current(moved.data())).containsExactlyInAnyOrderEntriesOf(Map.of("A", "40", "B", "40", "C", "20"));
        assertThat(moved.data().path("transfer").path("reference").asString()).isEqualTo("Deed 12");
        assertThat(moved.data().path("revision").asLong()).isEqualTo(3);

        int periods = rows("ownership_period");
        var refused = transfer("A", "C", "50", 3, UUID.randomUUID());
        assertThat(refused.code()).isEqualTo("INSUFFICIENT_SOURCE_SHARE");
        assertThat(rows("ownership_period")).isEqualTo(periods);
        assertThat(rows("ownership_transfer")).isEqualTo(1);
        assertThat(rows("building_outbox")).isEqualTo(1);

        var sameDay = transfer("A", "C", "40", 3, UUID.randomUUID());
        assertThat(current(sameDay.data())).containsExactlyInAnyOrderEntriesOf(Map.of("B", "40", "C", "60"));
        var history = send("GET", "/ownership-history", admin, null).data();
        assertThat(history.path("periods")).hasSize(5);
        assertThat(history.path("transfers")).hasSize(2);
        assertThat(history.path("transfers").get(0).path("revision").asLong()).isEqualTo(3);
        assertThat(history.path("transfers").get(1).path("revision").asLong()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_audit WHERE building_id = ? "
                + "AND action = 'OWNERSHIP_TRANSFERRED'", Integer.class, building)).isEqualTo(2);
    }

    @Test
    void transferWritesItsOutboxEventInTheSameTransaction() throws Exception {
        assign("A", "100", 0, UUID.randomUUID());
        var moved = transfer("A", "B", "25.5", 1, UUID.randomUUID()).data();
        var event = jdbc.queryForMap("SELECT event_type, event_version, aggregate_id, aggregate_revision, "
                + "payload::text AS payload, delivered_at FROM building_outbox WHERE building_id = ?", building);
        assertThat(event.get("event_type")).isEqualTo("ownership.transferred");
        assertThat(event.get("event_version")).isEqualTo(1);
        assertThat(event.get("aggregate_id")).isEqualTo(unit);
        assertThat(event.get("aggregate_revision")).isEqualTo(2L);
        assertThat(event.get("delivered_at")).isNull();
        var payload = JSON.readTree((String) event.get("payload"));
        assertThat(payload.path("transferId").asString()).isEqualTo(moved.path("transfer").path("id").asString());
        assertThat(payload.path("share").decimalValue()).isEqualByComparingTo("25.5");
        assertThat(payload.path("effectiveDate").asString()).isEqualTo(today);
        assertThat(payload.toString()).doesNotContain("0171").doesNotContain("reason");
    }

    @Test
    void operationIdsReplayAndRulesGuardEveryWrite() throws Exception {
        UUID operation = UUID.randomUUID();
        assertThat(assign("A", "50", 0, operation).status()).isEqualTo(201);
        var replay = assign("A", "50", 0, operation);
        assertThat(replay.status()).isEqualTo(200);
        assertThat(replay.data().path("replayed").asBoolean()).isTrue();
        assertThat(assign("A", "51", 0, operation).code()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(rows("ownership_period")).isEqualTo(1);

        UUID stranger = UUID.randomUUID();
        assertThat(send("POST", "/ownerships", admin, "{\"ownerUserId\":\"" + stranger + "\",\"share\":10,"
                + "\"effectiveDate\":\"" + today + "\",\"expectedVersion\":1,\"operationId\":\"" + UUID.randomUUID()
                + "\"}").code()).isEqualTo("OWNER_NOT_MEMBER");
        String yesterday = LocalDate.parse(today).minusDays(1).toString();
        assertThat(send("POST", "/ownerships", admin, "{\"ownerUserId\":\"" + owners.get("B") + "\",\"share\":10,"
                + "\"effectiveDate\":\"" + yesterday + "\",\"expectedVersion\":1,\"operationId\":\"" + UUID.randomUUID()
                + "\"}").code()).isEqualTo("EFFECTIVE_DATE_NOT_TODAY");
        assertThat(assign("B", "10.00001", 1, UUID.randomUUID()).code()).isEqualTo("SHARE_PRECISION");
        assertThat(send("POST", "/ownership-transfers", admin,
                transferBody("A", "B", "10", 1, UUID.randomUUID()).replace("\"reason\":\"Sale\",", "")).status())
                .isEqualTo(400);
        assertThat(send("POST", "/ownerships", owners.get("A"), "{\"ownerUserId\":\"" + owners.get("B")
                + "\",\"share\":10,\"effectiveDate\":\"" + today + "\",\"expectedVersion\":1,\"operationId\":\""
                + UUID.randomUUID() + "\"}").status()).isEqualTo(403);
        jdbc.update("UPDATE building SET status = 'SUSPENDED' WHERE id = ?", building);
        assertThat(assign("B", "10", 1, UUID.randomUUID()).code()).isEqualTo("BUILDING_READ_ONLY");
    }

    @Test
    void ownersSeeOnlyTheirOwnHistory() throws Exception {
        assign("A", "60", 0, UUID.randomUUID());
        assign("B", "40", 1, UUID.randomUUID());
        transfer("A", "C", "60", 2, UUID.randomUUID());

        var formerOwner = send("GET", "/ownership-history", owners.get("A"), null).data();
        assertThat(formerOwner.path("periods")).hasSize(1);
        assertThat(formerOwner.path("periods").get(0).path("endRevision").asLong()).isEqualTo(3);
        assertThat(formerOwner.path("transfers")).hasSize(1);
        var coOwner = send("GET", "/ownership-history", owners.get("B"), null).data();
        assertThat(coOwner.path("periods")).hasSize(1);
        assertThat(coOwner.path("transfers")).isEmpty();
        assertThat(coOwner.toString()).doesNotContain(owners.get("A").toString());
        assertThat(send("GET", "/ownership-history", owners.get("D"), null).code()).isEqualTo("UNIT_NOT_FOUND");
        assertThat(send("GET", "/ownership-history", UUID.randomUUID(), null).code()).isEqualTo("BUILDING_NOT_FOUND");

        jdbc.update("UPDATE building_membership SET status = 'REVOKED', revoked_at = now(), revoked_by = ?, "
                + "revocation_reason = 'Sold', version = 1 WHERE user_id = ?", admin, owners.get("A"));
        assertThat(send("GET", "/ownership-history", owners.get("A"), null).status()).isEqualTo(404);
    }

    @Test
    void concurrentAllocationsNeverExceedOneHundredPercent() throws Exception {
        assign("A", "90", 0, UUID.randomUUID());
        int callers = 2;
        var barrier = new CyclicBarrier(callers);
        var pool = Executors.newFixedThreadPool(callers);
        try {
            List<Future<Reply>> replies = new ArrayList<>();
            for (String owner : List.of("B", "C")) {
                replies.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return assign(owner, "10", 1, UUID.randomUUID());
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (var reply : replies) {
                statuses.add(reply.get(30, TimeUnit.SECONDS).status());
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT sum(share) FROM ownership_period WHERE unit_id = ? "
                + "AND end_revision IS NULL", java.math.BigDecimal.class, unit)).isEqualByComparingTo("100");
    }
}
