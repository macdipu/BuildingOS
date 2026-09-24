package com.buildingos.building.buildingapplication;

import static org.assertj.core.api.Assertions.assertThat;

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
class BuildingApplicationApiIntegrationTest {
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

    private final UUID applicant = UUID.randomUUID();
    private final UUID reviewer = UUID.randomUUID();

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

    private String applicantToken() throws Exception { return token(applicant); }

    private String reviewerToken() throws Exception { return token(reviewer, "PLATFORM_ADMIN"); }

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

    private static final String COMPLETE = """
            {"buildingName":"Rose Garden","buildingType":"residential","address":"House 12, Road 5",
             "area":"Dhanmondi","district":"Dhaka","estimatedUnits":36,"totalFloors":10,
             "applicantRelationship":"OWNER","contactName":"Rahim Uddin","contactPhone":"+8801712345678",
             "managementType":"OWNERS_COMMITTEE","latitude":23.7461,"longitude":90.3742}""";

    private String createSubmitted() throws Exception {
        var created = send("POST", "/api/v1/building-applications", applicantToken(), COMPLETE);
        assertThat(created.status()).isEqualTo(201);
        String id = created.data().path("id").asString();
        assertThat(send("POST", "/api/v1/building-applications/" + id + "/submit", applicantToken(), null).status())
                .isEqualTo(200);
        return id;
    }

    private String underReview() throws Exception {
        String id = createSubmitted();
        assertThat(send("POST", "/api/v1/platform/building-applications/" + id + "/start-review", reviewerToken(), null)
                .status()).isEqualTo(200);
        return id;
    }

    @Test
    void draftIsEditedThenSubmittedWithCanonicalFields() throws Exception {
        var created = send("POST", "/api/v1/building-applications", applicantToken(), "{\"buildingName\":\"Rose\"}");
        assertThat(created.status()).isEqualTo(201);
        assertThat(created.data().path("status").asString()).isEqualTo("DRAFT");
        assertThat(created.data().path("applicationNumber").asString()).matches("BA-\\d{4}-\\d{6}");
        assertThat(created.data().path("missingFields").size()).isEqualTo(8);
        String id = created.data().path("id").asString();

        var early = send("POST", "/api/v1/building-applications/" + id + "/submit", applicantToken(), null);
        assertThat(early.status()).isEqualTo(400);
        assertThat(early.code()).isEqualTo("APPLICATION_INCOMPLETE");

        var edited = send("PUT", "/api/v1/building-applications/" + id, applicantToken(), COMPLETE);
        assertThat(edited.status()).isEqualTo(200);
        assertThat(edited.data().path("contactPhone").asString()).isEqualTo("01712345678");
        assertThat(edited.data().path("buildingType").asString()).isEqualTo("RESIDENTIAL");
        assertThat(edited.data().path("missingFields").size()).isZero();

        var submitted = send("POST", "/api/v1/building-applications/" + id + "/submit", applicantToken(), null);
        assertThat(submitted.data().path("status").asString()).isEqualTo("SUBMITTED");
        var locked = send("PUT", "/api/v1/building-applications/" + id, applicantToken(), COMPLETE);
        assertThat(locked.status()).isEqualTo(409);
        assertThat(locked.code()).isEqualTo("NOT_EDITABLE");

        var mine = send("GET", "/api/v1/me/building-applications", applicantToken(), null);
        assertThat(mine.data().findValuesAsString("id")).contains(id);
    }

    @Test
    void invalidFieldsAre400() throws Exception {
        var badPhone = send("POST", "/api/v1/building-applications", applicantToken(), "{\"contactPhone\":\"12345\"}");
        assertThat(badPhone.status()).isEqualTo(400);
        assertThat(badPhone.code()).isEqualTo("INVALID_REQUEST");
        var badEnum = send("POST", "/api/v1/building-applications", applicantToken(), "{\"buildingType\":\"CASTLE\"}");
        assertThat(badEnum.status()).isEqualTo(400);
        var halfPoint = send("POST", "/api/v1/building-applications", applicantToken(), "{\"latitude\":23.7}");
        assertThat(halfPoint.status()).isEqualTo(400);
    }

    @Test
    void applicationsAreIsolatedPerApplicant() throws Exception {
        String id = createSubmitted();
        String stranger = token(UUID.randomUUID());
        assertThat(send("GET", "/api/v1/building-applications/" + id, stranger, null).code())
                .isEqualTo("APPLICATION_NOT_FOUND");
        assertThat(send("PUT", "/api/v1/building-applications/" + id, stranger, COMPLETE).status()).isEqualTo(404);
        assertThat(send("GET", "/api/v1/building-applications/" + id + "/history", stranger, null).status())
                .isEqualTo(404);
        assertThat(send("GET", "/api/v1/me/building-applications", stranger, null).data().size()).isZero();

        var asAdmin = send("GET", "/api/v1/building-applications/" + id, reviewerToken(), null);
        assertThat(asAdmin.status()).isEqualTo(200);
    }

    @Test
    void reviewActionsNeedPlatformAdmin() throws Exception {
        String id = createSubmitted();
        for (String roleless : List.of(applicantToken(), token(UUID.randomUUID(), "SUBSCRIPTION_ADMIN"))) {
            assertThat(send("POST", "/api/v1/platform/building-applications/" + id + "/start-review", roleless, null)
                    .status()).isEqualTo(403);
            assertThat(send("GET", "/api/v1/platform/building-applications", roleless, null).status()).isEqualTo(403);
            assertThat(send("GET", "/api/v1/platform/building-applications/" + id + "/notes", roleless, null).status())
                    .isEqualTo(403);
        }
        assertThat(send("GET", "/api/v1/platform/building-applications", null, null).status()).isEqualTo(401);
        assertThat(send("POST", "/api/v1/building-applications", null, COMPLETE).status()).isEqualTo(401);
        var superAdmin = token(UUID.randomUUID(), "SUPER_ADMIN");
        assertThat(send("POST", "/api/v1/platform/building-applications/" + id + "/start-review", superAdmin, null)
                .status()).isEqualTo(200);
    }

    @Test
    void moreInformationRoundTripIsAuditedAndVisibleToApplicant() throws Exception {
        String id = underReview();
        var noMessage = send("POST", "/api/v1/platform/building-applications/" + id + "/request-information",
                reviewerToken(), "{\"message\":\" \"}");
        assertThat(noMessage.status()).isEqualTo(400);

        var info = send("POST", "/api/v1/platform/building-applications/" + id + "/request-information",
                reviewerToken(), "{\"message\":\"Please upload the holding tax receipt\"}");
        assertThat(info.data().path("status").asString()).isEqualTo("MORE_INFORMATION_REQUIRED");

        var seen = send("GET", "/api/v1/building-applications/" + id, applicantToken(), null);
        assertThat(seen.data().path("infoRequestMessage").asString()).isEqualTo("Please upload the holding tax receipt");
        assertThat(seen.data().path("reviewedBy").isNull()).isTrue();

        assertThat(send("PUT", "/api/v1/building-applications/" + id, applicantToken(), COMPLETE).status())
                .isEqualTo(200);
        assertThat(send("POST", "/api/v1/building-applications/" + id + "/submit", applicantToken(), null)
                .data().path("status").asString()).isEqualTo("SUBMITTED");

        var history = send("GET", "/api/v1/building-applications/" + id + "/history", applicantToken(), null).data();
        var statuses = new ArrayList<String>();
        history.forEach(t -> statuses.add(t.path("toStatus").asString()));
        assertThat(statuses).containsExactly("DRAFT", "SUBMITTED", "UNDER_REVIEW", "MORE_INFORMATION_REQUIRED",
                "SUBMITTED");
        assertThat(history.get(3).path("reason").asString()).isEqualTo("Please upload the holding tax receipt");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lifecycle_transition WHERE entity_id = ?::uuid "
                + "AND actor_user_id = ?", Integer.class, id, reviewer)).isEqualTo(2);
    }

    @Test
    void rejectNeedsReasonAndIsFinal() throws Exception {
        String id = underReview();
        assertThat(send("POST", "/api/v1/platform/building-applications/" + id + "/reject", reviewerToken(),
                "{}").status()).isEqualTo(400);
        var rejected = send("POST", "/api/v1/platform/building-applications/" + id + "/reject", reviewerToken(),
                "{\"reason\":\"Building already registered\"}");
        assertThat(rejected.data().path("status").asString()).isEqualTo("REJECTED");
        assertThat(rejected.data().path("reviewedBy").asString()).isEqualTo(reviewer.toString());
        var again = send("POST", "/api/v1/platform/building-applications/" + id + "/start-review", reviewerToken(), null);
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.code()).isEqualTo("INVALID_TRANSITION");
        assertThat(send("GET", "/api/v1/building-applications/" + id, applicantToken(), null)
                .data().path("rejectionReason").asString()).isEqualTo("Building already registered");
    }

    @Test
    void notesAreAdminOnly() throws Exception {
        String id = createSubmitted();
        var added = send("POST", "/api/v1/platform/building-applications/" + id + "/notes", reviewerToken(),
                "{\"body\":\"Called applicant, deed pending\"}");
        assertThat(added.status()).isEqualTo(201);
        assertThat(send("POST", "/api/v1/platform/building-applications/" + id + "/notes", reviewerToken(),
                "{\"body\":\"\"}").status()).isEqualTo(400);
        var notes = send("GET", "/api/v1/platform/building-applications/" + id + "/notes", reviewerToken(), null);
        assertThat(notes.data().findValuesAsString("body")).containsExactly("Called applicant, deed pending");

        var applicantView = send("GET", "/api/v1/building-applications/" + id, applicantToken(), null).body().toString();
        assertThat(applicantView).doesNotContain("deed pending");
        var history = send("GET", "/api/v1/building-applications/" + id + "/history", applicantToken(), null)
                .body().toString();
        assertThat(history).doesNotContain("deed pending");
        assertThat(send("GET", "/api/v1/platform/building-applications/" + UUID.randomUUID() + "/notes",
                reviewerToken(), null).code()).isEqualTo("APPLICATION_NOT_FOUND");
    }

    @Test
    void reviewQueueHidesDraftsAndPages() throws Exception {
        send("POST", "/api/v1/building-applications", applicantToken(), "{}");
        String submitted = createSubmitted();
        var queue = send("GET", "/api/v1/platform/building-applications?status=SUBMITTED&size=100", reviewerToken(), null);
        assertThat(queue.status()).isEqualTo(200);
        assertThat(queue.data().findValuesAsString("id")).contains(submitted);
        assertThat(queue.data().findValuesAsString("status")).containsOnly("SUBMITTED");
        assertThat(queue.body().path("meta").path("total").asLong()).isGreaterThanOrEqualTo(1);

        var all = send("GET", "/api/v1/platform/building-applications?size=100", reviewerToken(), null);
        assertThat(all.data().findValuesAsString("status")).doesNotContain("DRAFT");
        assertThat(send("GET", "/api/v1/platform/building-applications?status=DRAFT", reviewerToken(), null).status())
                .isEqualTo(400);
        assertThat(send("GET", "/api/v1/platform/building-applications?size=101", reviewerToken(), null).status())
                .isEqualTo(400);
    }

    @Test
    void concurrentStartReviewHasOneWinner() throws Exception {
        String id = createSubmitted();
        int racers = 4;
        var barrier = new CyclicBarrier(racers);
        var pool = Executors.newFixedThreadPool(racers);
        try {
            var results = new ArrayList<Future<Integer>>();
            for (int i = 0; i < racers; i++) {
                String racer = token(UUID.randomUUID(), "PLATFORM_ADMIN");
                results.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return send("POST", "/api/v1/platform/building-applications/" + id + "/start-review", racer, null)
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
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lifecycle_transition WHERE entity_id = ?::uuid "
                + "AND to_status = 'UNDER_REVIEW'", Integer.class, id)).isEqualTo(1);
    }
}
