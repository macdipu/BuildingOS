package com.buildingos.building.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.support.JwtFixtures;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class DocumentApiIntegrationTest {
    private static final String AUDIENCE = "building-platform";
    private static final String BUCKET = "building-documents-test";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static JwtFixtures fixtures;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("building_db").withUsername("building_app").withPassword("test-only-password");

    @Container
    static final MinIOContainer MINIO = new MinIOContainer(DockerImageName.parse(
            "quay.io/minio/minio@sha256:14cea493d9a34af32f524e538b8346cf79f3321eff8e708c1e2960462bd8936e")
            .asCompatibleSubstituteFor("minio/minio"));

    @LocalServerPort
    private int port;

    @Autowired
    private S3Client s3;

    private final UUID applicant = UUID.randomUUID();

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
        registry.add("buildingos.documents.max-per-application", () -> "3");
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private String token(UUID user, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of(roles));
    }

    private void ensureBucket() {
        if (s3.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(BUCKET))) {
            s3.createBucket(b -> b.bucket(BUCKET));
        }
    }

    private Reply json(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        return parse(HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString()));
    }

    private Reply upload(String applicationId, String token, String fileName, String declaredType, byte[] content)
            throws Exception {
        String boundary = "----bos" + UUID.randomUUID();
        var body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                + "\"\r\nContent-Type: " + declaredType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/api/v1/building-applications/" + applicationId + "/documents"))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + token).build();
        return parse(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private static Reply parse(HttpResponse<String> response) throws Exception {
        return new Reply(response.statusCode(), response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private String draft() throws Exception {
        ensureBucket();
        return json("POST", "/api/v1/building-applications", token(applicant), "{}").data().path("id").asString();
    }

    @Test
    void uploadListDownloadRoundTrip() throws Exception {
        String id = draft();
        var stored = upload(id, token(applicant), "../deed.pdf", "image/png", DocumentPolicyTest.PDF);
        assertThat(stored.status()).isEqualTo(201);
        assertThat(stored.data().path("contentType").asString()).isEqualTo("application/pdf");
        assertThat(stored.data().path("fileName").asString()).isEqualTo("deed.pdf");
        String docId = stored.data().path("id").asString();

        var listed = json("GET", "/api/v1/building-applications/" + id + "/documents", token(applicant), null);
        assertThat(listed.data().findValuesAsString("id")).containsExactly(docId);

        for (String reader : List.of(token(applicant), token(UUID.randomUUID(), "PLATFORM_ADMIN"))) {
            var response = HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                            + "/api/v1/building-applications/" + id + "/documents/" + docId))
                    .header("Authorization", "Bearer " + reader).build(), HttpResponse.BodyHandlers.ofByteArray());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(DocumentPolicyTest.PDF);
            assertThat(response.headers().firstValue("Content-Type")).hasValue("application/pdf");
            assertThat(response.headers().firstValue("Content-Disposition").orElseThrow())
                    .startsWith("attachment").contains("deed.pdf");
            assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        }
        assertThat(s3.headObject(h -> h.bucket(BUCKET).key("applications/" + id + "/" + docId)).contentLength())
                .isEqualTo(DocumentPolicyTest.PDF.length);
    }

    @Test
    void rejectsWrongTypeSizeAndCount() throws Exception {
        String id = draft();
        var html = upload(id, token(applicant), "x.pdf", "application/pdf", "<html>".getBytes(StandardCharsets.UTF_8));
        assertThat(html.status()).isEqualTo(415);
        assertThat(html.code()).isEqualTo("UNSUPPORTED_DOCUMENT_TYPE");

        byte[] big = new byte[5000];
        System.arraycopy(DocumentPolicyTest.PDF, 0, big, 0, DocumentPolicyTest.PDF.length);
        var tooBig = upload(id, token(applicant), "big.pdf", "application/pdf", big);
        assertThat(tooBig.status()).isEqualTo(413);
        assertThat(tooBig.code()).isEqualTo("DOCUMENT_TOO_LARGE");

        for (int i = 0; i < 3; i++) {
            assertThat(upload(id, token(applicant), "p" + i + ".png", "image/png", DocumentPolicyTest.PNG).status())
                    .isEqualTo(201);
        }
        var fourth = upload(id, token(applicant), "p4.jpg", "image/jpeg", DocumentPolicyTest.JPEG);
        assertThat(fourth.status()).isEqualTo(409);
        assertThat(fourth.code()).isEqualTo("DOCUMENT_LIMIT_REACHED");
    }

    @Test
    void onlyTheApplicantChangesDocumentsAndOnlyWhileEditable() throws Exception {
        String id = draft();
        String admin = token(UUID.randomUUID(), "PLATFORM_ADMIN");
        assertThat(upload(id, admin, "a.pdf", "application/pdf", DocumentPolicyTest.PDF).status()).isEqualTo(404);
        assertThat(upload(id, token(UUID.randomUUID()), "a.pdf", "application/pdf", DocumentPolicyTest.PDF).status())
                .isEqualTo(404);
        assertThat(json("GET", "/api/v1/building-applications/" + id + "/documents", token(UUID.randomUUID()), null)
                .status()).isEqualTo(404);

        String docId = upload(id, token(applicant), "a.pdf", "application/pdf", DocumentPolicyTest.PDF)
                .data().path("id").asString();
        json("PUT", "/api/v1/building-applications/" + id, token(applicant), """
                {"buildingName":"Rose Garden","buildingType":"RESIDENTIAL","address":"House 12","area":"Dhanmondi",
                 "district":"Dhaka","estimatedUnits":36,"applicantRelationship":"OWNER","contactName":"Rahim",
                 "contactPhone":"01712345678"}""");
        assertThat(json("POST", "/api/v1/building-applications/" + id + "/submit", token(applicant), null).status())
                .isEqualTo(200);

        var late = upload(id, token(applicant), "b.pdf", "application/pdf", DocumentPolicyTest.PDF);
        assertThat(late.status()).isEqualTo(409);
        assertThat(late.code()).isEqualTo("NOT_EDITABLE");
        assertThat(json("DELETE", "/api/v1/building-applications/" + id + "/documents/" + docId, token(applicant), null)
                .status()).isEqualTo(409);
    }

    @Test
    void removeDeletesRowAndObject() throws Exception {
        String id = draft();
        String docId = upload(id, token(applicant), "a.pdf", "application/pdf", DocumentPolicyTest.PDF)
                .data().path("id").asString();
        assertThat(json("DELETE", "/api/v1/building-applications/" + id + "/documents/" + docId, token(applicant), null)
                .status()).isEqualTo(204);
        assertThat(json("GET", "/api/v1/building-applications/" + id + "/documents", token(applicant), null)
                .data().size()).isZero();
        assertThatThrownBy(() -> s3.headObject(h -> h.bucket(BUCKET).key("applications/" + id + "/" + docId)))
                .isInstanceOf(NoSuchKeyException.class);
        assertThat(json("DELETE", "/api/v1/building-applications/" + id + "/documents/" + docId, token(applicant), null)
                .code()).isEqualTo("DOCUMENT_NOT_FOUND");
    }
}
