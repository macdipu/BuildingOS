package com.buildingos.building.ownership;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.JwtFixtures;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
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
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** F4-T5b: transfer documents: party-scoped access, content boundary, audited removal, storage failure. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class TransferDocumentApiIntegrationTest {
    private static final byte[] PDF = "%PDF-1.7\n1 0 obj".getBytes(StandardCharsets.US_ASCII);
    private static final String AUDIENCE = "building-platform";
    private static final String BUCKET = "transfer-documents-test";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    // quay.io/minio/minio and docker.io/minio/minio now deny anonymous pulls (upstream
    // MinIO registry restriction). bitnamilegacy/minio is a frozen but pullable,
    // drop-in-compatible build; MinIOContainer's command override (`server /data`)
    // still works against its entrypoint.
    @Container
    static final MinIOContainer MINIO = new MinIOContainer(DockerImageName.parse(
            "bitnamilegacy/minio@sha256:451fe6858cb770cc9d0e77ba811ce287420f781c7c1b806a386f6896471a349c")
            .asCompatibleSubstituteFor("minio/minio"));

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private S3Client s3;

    private UUID building;
    private UUID unit;
    private UUID otherUnit;
    private UUID admin;
    private UUID transfer;
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
        registry.add("buildingos.documents.s3.endpoint", MINIO::getS3URL);
        registry.add("buildingos.documents.s3.bucket", () -> BUCKET);
        registry.add("buildingos.documents.s3.access-key", MINIO::getUserName);
        registry.add("buildingos.documents.s3.secret-key", MINIO::getPassword);
        registry.add("buildingos.documents.s3.path-style", () -> "true");
        registry.add("buildingos.documents.max-size-bytes", () -> "4096");
        registry.add("buildingos.ownership.max-documents-per-transfer", () -> "2");
    }

    @BeforeEach
    void seed() throws Exception {
        if (s3.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(BUCKET))) {
            s3.createBucket(b -> b.bucket(BUCKET));
        }
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
        unit = unit(floor, "4B");
        otherUnit = unit(floor, "4C");
        for (String name : List.of("A", "B", "C")) {
            owners.put(name, UUID.randomUUID());
            member(owners.get(name), "OWNER");
        }
        send("POST", unitPath(unit) + "/ownerships", admin, "{\"ownerUserId\":\"" + owners.get("A")
                + "\",\"share\":100,\"effectiveDate\":\"" + today + "\",\"expectedVersion\":0,\"operationId\":\""
                + UUID.randomUUID() + "\"}");
        var moved = send("POST", unitPath(unit) + "/ownership-transfers", admin, "{\"sourceOwnerUserId\":\""
                + owners.get("A") + "\",\"recipientUserId\":\"" + owners.get("B") + "\",\"share\":30,"
                + "\"effectiveDate\":\"" + today + "\",\"reason\":\"Sale\",\"expectedVersion\":1,\"operationId\":\""
                + UUID.randomUUID() + "\"}");
        assertThat(moved.status()).isEqualTo(201);
        transfer = UUID.fromString(moved.data().path("transfer").path("id").asString());
    }

    @Test
    void adminAttachesAndOnlyAdminsAndPartiesRead() throws Exception {
        var stored = upload(admin, "../deed.pdf", PDF);
        assertThat(stored.status()).isEqualTo(201);
        String documentId = stored.data().path("id").asString();
        assertThat(stored.data().path("fileName").asString()).isEqualTo("deed.pdf");
        assertThat(stored.data().path("contentType").asString()).isEqualTo("application/pdf");
        assertThat(s3.headObject(h -> h.bucket(BUCKET).key("ownership-transfers/" + transfer + "/" + documentId))
                .contentLength()).isEqualTo(PDF.length);
        assertThat(audits("TRANSFER_DOCUMENT_ATTACHED")).isEqualTo(1);

        for (UUID reader : List.of(admin, owners.get("A"), owners.get("B"))) {
            assertThat(send("GET", documentsPath(), reader, null).data()).hasSize(1);
            var file = download(reader, documentId);
            assertThat(file.statusCode()).isEqualTo(200);
            assertThat(file.body()).isEqualTo(PDF);
            assertThat(file.headers().firstValue("Content-Disposition").orElseThrow()).startsWith("attachment");
            assertThat(file.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        }

        UUID unrelatedOwner = owners.get("C");
        assertThat(send("GET", documentsPath(), unrelatedOwner, null).code()).isEqualTo("TRANSFER_NOT_FOUND");
        assertThat(download(unrelatedOwner, documentId).statusCode()).isEqualTo(404);
        assertThat(send("GET", documentsPath(), UUID.randomUUID(), null).status()).isEqualTo(404);
        assertThat(upload(owners.get("B"), "b.pdf", PDF).status()).isEqualTo(403);
        assertThat(send("GET", unitPath(otherUnit) + "/ownership-transfers/" + transfer + "/documents", admin, null)
                .code()).isEqualTo("TRANSFER_NOT_FOUND");

        jdbc.update("UPDATE building_membership SET status = 'REVOKED', revoked_at = now(), revoked_by = ?, "
                + "revocation_reason = 'Left' WHERE building_id = ? AND user_id = ?", admin, building, owners.get("B"));
        assertThat(send("GET", documentsPath(), owners.get("B"), null).status()).isEqualTo(404);
        assertThat(download(owners.get("B"), documentId).statusCode()).isEqualTo(404);
    }

    @Test
    void contentBoundaryAndPerTransferLimitRejectWithoutState() throws Exception {
        assertThat(upload(admin, "note.pdf", "plain text".getBytes(StandardCharsets.UTF_8)).code())
                .isEqualTo("UNSUPPORTED_DOCUMENT_TYPE");
        byte[] big = new byte[5000];
        System.arraycopy(PDF, 0, big, 0, PDF.length);
        assertThat(upload(admin, "big.pdf", big).status()).isEqualTo(413);
        assertThat(documentRows()).isZero();
        assertThat(objects()).isZero();

        assertThat(upload(admin, "1.pdf", PDF).status()).isEqualTo(201);
        assertThat(upload(admin, "2.pdf", PDF).status()).isEqualTo(201);
        assertThat(upload(admin, "3.pdf", PDF).code()).isEqualTo("DOCUMENT_LIMIT_REACHED");
        assertThat(documentRows()).isEqualTo(2);
        assertThat(objects()).isEqualTo(2);
    }

    @Test
    void removalIsAuditedKeepsTheTransferAndDeletesTheBytes() throws Exception {
        String documentId = upload(admin, "deed.pdf", PDF).data().path("id").asString();
        String path = documentsPath() + "/" + documentId;

        assertThat(send("DELETE", path, admin, "{}").status()).isEqualTo(400);
        assertThat(send("DELETE", path, owners.get("B"), "{\"reason\":\"Wrong file\"}").status()).isEqualTo(403);
        assertThat(send("DELETE", path, admin, "{\"reason\":\"Wrong file\"}").status()).isEqualTo(204);

        assertThat(send("GET", documentsPath(), admin, null).data()).isEmpty();
        assertThat(download(admin, documentId).statusCode()).isEqualTo(404);
        assertThat(send("DELETE", path, admin, "{\"reason\":\"Again\"}").code()).isEqualTo("DOCUMENT_NOT_FOUND");
        var row = jdbc.queryForMap("SELECT removed_by, removal_reason FROM ownership_document WHERE id = ?::uuid",
                documentId);
        assertThat(row.get("removed_by")).isEqualTo(admin);
        assertThat(row.get("removal_reason")).isEqualTo("Wrong file");
        assertThat(audits("TRANSFER_DOCUMENT_REMOVED")).isEqualTo(1);
        assertThat(objects()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ownership_transfer WHERE id = ?", Integer.class, transfer))
                .isEqualTo(1);
    }

    @Test
    void storageFailureIsRetryableAndLeavesNoMetadata() throws Exception {
        s3.listObjectsV2(l -> l.bucket(BUCKET)).contents()
                .forEach(o -> s3.deleteObject(d -> d.bucket(BUCKET).key(o.key())));
        s3.deleteBucket(b -> b.bucket(BUCKET));

        var refused = upload(admin, "deed.pdf", PDF);

        assertThat(refused.status()).isEqualTo(503);
        assertThat(refused.body().toString()).doesNotContain("NoSuchBucket").doesNotContain(BUCKET);
        assertThat(documentRows()).isZero();
        assertThat(audits("TRANSFER_DOCUMENT_ATTACHED")).isZero();
    }

    private UUID unit(UUID floor, String number) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO building_unit (id, building_id, floor_id, number, normalized_number, unit_type, "
                + "area_sqft, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'FLAT', 1200, now(), now())",
                id, building, floor, number, number);
        return id;
    }

    private void member(UUID user, String role) {
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE', now())", UUID.randomUUID(), building, user, role);
    }

    private String unitPath(UUID unitId) {
        return "/api/v1/buildings/" + building + "/units/" + unitId;
    }

    private String documentsPath() {
        return unitPath(unit) + "/ownership-transfers/" + transfer + "/documents";
    }

    private int documentRows() {
        return jdbc.queryForObject("SELECT count(*) FROM ownership_document WHERE transfer_id = ?", Integer.class,
                transfer);
    }

    private int objects() {
        return s3.listObjectsV2(l -> l.bucket(BUCKET).prefix("ownership-transfers/" + transfer + "/")).keyCount();
    }

    private int audits(String action) {
        return jdbc.queryForObject("SELECT count(*) FROM building_audit WHERE building_id = ? AND action = ?",
                Integer.class, building, action);
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private String token(UUID user) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of());
    }

    private Reply send(String method, String path, UUID user, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token(user)).build();
        return parse(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private HttpResponse<byte[]> download(UUID user, String documentId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + documentsPath() + "/"
                + documentId)).GET().header("Authorization", "Bearer " + token(user)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private Reply upload(UUID user, String fileName, byte[] content) throws Exception {
        String boundary = "----bos" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + documentsPath()))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + token(user)).build();
        return parse(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private static Reply parse(HttpResponse<String> response) throws Exception {
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }
}
