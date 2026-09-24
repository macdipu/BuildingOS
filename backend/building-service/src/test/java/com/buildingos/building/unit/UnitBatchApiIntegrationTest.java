package com.buildingos.building.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
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

/** F4-T3a: batch preview (rows, generator, CSV) and all-or-nothing, idempotent commit. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class UnitBatchApiIntegrationTest {
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
    private UUID admin;
    private String adminToken;
    private String first;
    private String second;

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
    void seedBuildingWithFloors() throws Exception {
        admin = UUID.randomUUID();
        building = seedBuilding();
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, 'BUILDING_ADMIN', 'ACTIVE', now())", UUID.randomUUID(), building, admin);
        adminToken = fixtures.userToken(AUDIENCE, admin, List.of());
        first = floor("1st Floor", 1);
        second = floor("2nd Floor", 2);
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private UUID seedBuilding() {
        Instant now = Instant.now();
        UUID application = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(), Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) VALUES (?, ?, 'Rose Tower', 'RESIDENTIAL', "
                + "'Road 1', 'Mirpur', 'Dhaka', '01712345678', 'ONBOARDING', 0, ?, ?)",
                id, application, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private String floor(String label, int order) throws Exception {
        return send("POST", "/floors", "{\"label\":\"" + label + "\",\"kind\":\"REGULAR\",\"displayOrder\":" + order
                + "}").data().path("id").asString();
    }

    private Reply send(String method, String path, String body) throws Exception {
        return send(method, path, body, "application/json", adminToken);
    }

    private Reply send(String method, String path, String body, String contentType, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/buildings/" + building
                        + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", contentType).header("Authorization", "Bearer " + token);
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private Reply upload(String filename, String contentType, String content) throws Exception {
        String boundary = "unit-batch-" + UUID.randomUUID();
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                + "\"\r\nContent-Type: " + contentType + "\r\n\r\n" + content + "\r\n--" + boundary + "--\r\n";
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/buildings/" + building
                        + "/unit-batches/preview"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + adminToken).build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), JSON.readTree(response.body()));
    }

    private String row(String number, String floorId, String area) {
        return "{\"number\":\"" + number + "\",\"floorId\":\"" + floorId + "\",\"type\":\"FLAT\",\"areaSqft\":" + area
                + "}";
    }

    private Reply commit(UUID operation, String... rows) throws Exception {
        return send("POST", "/unit-batches/commit", "{\"operationId\":\"" + operation + "\",\"rows\":["
                + String.join(",", rows) + "]}");
    }

    private int units() {
        return jdbc.queryForObject("SELECT count(*) FROM building_unit WHERE building_id = ?", Integer.class, building);
    }

    private static List<String> codes(JsonNode rows) {
        List<String> codes = new ArrayList<>();
        rows.forEach(r -> codes.add(r.path("errors").isEmpty() ? "OK" : r.path("errors").get(0).path("code").asString()));
        return codes;
    }

    @Test
    void previewReportsEveryRowAndCommitIsAllOrNothing() throws Exception {
        assertThat(commit(UUID.randomUUID(), row("1A", first, "900")).status()).isEqualTo(201);
        var preview = send("POST", "/unit-batches/preview", "{\"rows\":[" + String.join(",", row("1B", first, "900"),
                row(" 1b ", first, "900"), row("1a", first, "900"), row("1C", first, "0"),
                row("1D", UUID.randomUUID().toString(), "900"),
                "{\"number\":\"1E\",\"floorLabel\":\"2ND FLOOR\",\"type\":\"VILLA\",\"areaSqft\":9}",
                "{\"number\":\"1F\",\"floorLabel\":\"2nd floor\",\"type\":\"PARKING\",\"areaSqft\":120}") + "]}");
        assertThat(preview.status()).isEqualTo(200);
        assertThat(preview.data().path("valid").asBoolean()).isFalse();
        assertThat(codes(preview.data().path("rows"))).containsExactly("OK", "DUPLICATE_IN_BATCH",
                "UNIT_NUMBER_TAKEN", "AREA_NOT_POSITIVE", "FLOOR_NOT_FOUND", "UNIT_TYPE_INVALID", "OK");
        assertThat(preview.data().path("rows").get(6).path("floorId").asString()).isEqualTo(second);
        assertThat(units()).isEqualTo(1);

        var rejected = commit(UUID.randomUUID(), row("2A", second, "900"), row("2B", second, "-1"));
        assertThat(rejected.status()).isEqualTo(400);
        assertThat(rejected.body().path("success").asBoolean()).isFalse();
        assertThat(rejected.body().path("meta").path("code").asString()).isEqualTo("BATCH_INVALID");
        assertThat(codes(rejected.data().path("rows"))).containsExactly("OK", "AREA_NOT_POSITIVE");
        var conflict = commit(UUID.randomUUID(), row("2A", second, "900"), row("1A", second, "900"));
        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.body().path("meta").path("code").asString()).isEqualTo("BATCH_CONFLICT");
        assertThat(units()).isEqualTo(1);
    }

    @Test
    void commitIsIdempotentPerOperationId() throws Exception {
        UUID operation = UUID.randomUUID();
        var created = commit(operation, row("3A", first, "900"), row("3B", first, "950.25"));
        assertThat(created.status()).isEqualTo(201);
        String batch = created.data().path("batchId").asString();
        assertThat(created.data().path("unitCount").asInt()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_audit WHERE building_id = ? "
                + "AND action = 'UNIT_CREATED'", Integer.class, building)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_audit WHERE entity_id = ?::uuid "
                + "AND action = 'UNIT_BATCH_COMMITTED'", Integer.class, batch)).isEqualTo(1);

        var replay = commit(operation, row("3A", first, "900"), row("3B", first, "950.25"));
        assertThat(replay.status()).isEqualTo(200);
        assertThat(replay.data().path("batchId").asString()).isEqualTo(batch);
        assertThat(replay.data().path("replayed").asBoolean()).isTrue();
        assertThat(replay.data().path("unitCount").asInt()).isEqualTo(2);
        assertThat(commit(operation, row("3C", first, "900")).code()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(units()).isEqualTo(2);
    }

    @Test
    void generatorExpandsPatternsAndDuplicatesATemplateFloor() throws Exception {
        var generated = send("POST", "/unit-batches/preview", "{\"generate\":{\"floorIds\":[\"" + first + "\",\""
                + second + "\"],\"unitsPerFloor\":4,\"numberPattern\":\"{floor}{letter}\",\"type\":\"FLAT\","
                + "\"areaSqft\":1100,\"bedrooms\":3}}");
        var rows = generated.data().path("rows");
        assertThat(generated.data().path("valid").asBoolean()).isTrue();
        assertThat(rows).hasSize(8);
        assertThat(rows.get(0).path("number").asString()).isEqualTo("1A");
        assertThat(rows.get(7).path("number").asString()).isEqualTo("2D");
        List<String> reviewed = new ArrayList<>();
        rows.forEach(r -> reviewed.add(r.toString()));
        assertThat(commit(UUID.randomUUID(), reviewed.subList(0, 4).toArray(String[]::new)).status()).isEqualTo(201);

        String third = floor("3rd Floor", 3);
        var copied = send("POST", "/unit-batches/preview", "{\"generate\":{\"floorIds\":[\"" + third + "\"],"
                + "\"numberPattern\":\"{floor}{nn}\",\"templateFloorId\":\"" + first + "\"}}");
        assertThat(copied.data().path("valid").asBoolean()).isTrue();
        assertThat(copied.data().path("rows")).hasSize(4);
        assertThat(copied.data().path("rows").get(0).path("number").asString()).isEqualTo("301");
        assertThat(copied.data().path("rows").get(0).path("bedrooms").asInt()).isEqualTo(3);
        assertThat(send("POST", "/unit-batches/preview", "{\"generate\":{\"floorIds\":[\"" + first + "\"],"
                + "\"unitsPerFloor\":501,\"numberPattern\":\"{n}\",\"type\":\"FLAT\",\"areaSqft\":1}}").code())
                .isEqualTo("UNITS_PER_FLOOR_INVALID");
    }

    @Test
    void csvUploadPreviewsRowsAndRejectsUnsafeSheets() throws Exception {
        var preview = upload("units.csv", "text/csv", "number,floor,type,areaSqft,bedrooms\n"
                + "5A,1st Floor,FLAT,900,2\n5B,Missing Floor,FLAT,900,2\n");
        assertThat(preview.status()).isEqualTo(200);
        assertThat(codes(preview.data().path("rows"))).containsExactly("OK", "FLOOR_NOT_FOUND");
        assertThat(upload("units.csv", "text/csv", "number,floor,type,areaSqft\n=1+1,1st Floor,FLAT,9\n").code())
                .isEqualTo("SHEET_FORMULA_NOT_ALLOWED");
        assertThat(upload("units.ods", "application/vnd.oasis.opendocument.spreadsheet", "PK").status())
                .isEqualTo(415);
        assertThat(upload("units.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "PK")
                .code()).isEqualTo("SHEET_INVALID");
        assertThat(upload("units.csv", "text/csv", "number,floor,type,areaSqft\n" + "x,1st Floor,FLAT,1\n".repeat(501))
                .code()).isEqualTo("BATCH_TOO_LARGE");
        assertThat(upload("units.csv", "text/csv", "a".repeat(1_048_577)).status()).isEqualTo(413);
    }

    @Test
    void concurrentCommitsNeverDuplicateNumbers() throws Exception {
        int callers = 4;
        var barrier = new CyclicBarrier(callers);
        var pool = Executors.newFixedThreadPool(callers);
        try {
            List<Future<Reply>> replies = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                String own = "9" + (char) ('A' + i);
                replies.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return commit(UUID.randomUUID(), row("9Z", second, "900"), row(own, second, "900"));
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (var reply : replies) {
                statuses.add(reply.get(30, TimeUnit.SECONDS).status());
            }
            assertThat(statuses).containsOnlyOnce(201).containsOnly(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(units()).isEqualTo(2);
    }

    @Test
    void batchesNeedAdminRightsAndAWritableBuilding() throws Exception {
        UUID owner = UUID.randomUUID();
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, 'OWNER', 'ACTIVE', now())", UUID.randomUUID(), building, owner);
        String body = "{\"operationId\":\"" + UUID.randomUUID() + "\",\"rows\":[" + row("7A", first, "900") + "]}";
        assertThat(send("POST", "/unit-batches/commit", body, "application/json",
                fixtures.userToken(AUDIENCE, owner, List.of())).status()).isEqualTo(403);
        assertThat(send("POST", "/unit-batches/preview", "{\"rows\":[],\"generate\":null}").code())
                .isEqualTo("BATCH_EMPTY");
        jdbc.update("UPDATE building SET status = 'SUSPENDED' WHERE id = ?", building);
        assertThat(send("POST", "/unit-batches/commit", body).code()).isEqualTo("BUILDING_READ_ONLY");
        assertThat(units()).isZero();
    }
}
