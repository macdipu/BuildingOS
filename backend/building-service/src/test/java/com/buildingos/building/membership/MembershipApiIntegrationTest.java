package com.buildingos.building.membership;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

/** F4-T1b: OWNER invitation, verified-phone claim, idempotency, revocation and per-request building access. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class MembershipApiIntegrationTest {
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
    private UUID adminMembership;
    private String phone;
    private final UUID recipient = UUID.randomUUID();

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
    void seedBuildingWithAdmin() {
        admin = UUID.randomUUID();
        building = seedBuilding("ONBOARDING");
        adminMembership = member(building, admin, "BUILDING_ADMIN");
        phone = randomPhone();
    }

    private record Reply(int status, JsonNode body) {
        String code() { return body.path("code").asString(); }
        JsonNode data() { return body.path("data"); }
    }

    private static String randomPhone() {
        return "0171" + (1000000 + (int) (Math.random() * 8999999));
    }

    private UUID seedBuilding(String status) {
        Instant now = Instant.now();
        UUID application = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO building_application (id, application_number, applicant_user_id, source, status, "
                + "version, created_at, updated_at) VALUES (?, ?, ?, 'SELF_SERVICE', 'APPROVED', 0, ?, ?)",
                application, UUID.randomUUID().toString().substring(0, 16), UUID.randomUUID(), Timestamp.from(now),
                Timestamp.from(now));
        jdbc.update("INSERT INTO building (id, application_id, name, building_type, address, area, district, "
                + "contact_phone, status, version, created_at, updated_at) VALUES (?, ?, 'Rose Tower', 'RESIDENTIAL', "
                + "'Road 1', 'Mirpur', 'Dhaka', '01712345678', ?, 0, ?, ?)",
                id, application, status, Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    private UUID member(UUID parent, UUID user, String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) "
                + "VALUES (?, ?, ?, ?, 'ACTIVE', now())", id, parent, user, role);
        return id;
    }

    private String token(UUID user, String phoneClaim, String... roles) throws Exception {
        return fixtures.userToken(AUDIENCE, user, List.of(roles), phoneClaim);
    }

    private String adminToken() throws Exception { return token(admin, null); }

    private String recipientToken() throws Exception { return token(recipient, phone); }

    private Reply send(String method, String path, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + token);
        var response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),
                response.body().isEmpty() ? JSON.createObjectNode() : JSON.readTree(response.body()));
    }

    private String base() { return "/api/v1/buildings/" + building; }

    private Reply invite(String token, String invitedPhone) throws Exception {
        return send("POST", base() + "/invitations", token,
                "{\"phone\":\"" + invitedPhone + "\",\"role\":\"OWNER\",\"reason\":\"Flat owner\"}");
    }

    private Reply claim(String token, String invitationId, UUID operationId) throws Exception {
        return send("POST", "/api/v1/me/building-invitations/" + invitationId + "/claim", token,
                "{\"operationId\":\"" + operationId + "\"}");
    }

    private int audits(String action, String entityId) {
        return jdbc.queryForObject("SELECT count(*) FROM building_audit WHERE action = ? AND entity_id = ?::uuid",
                Integer.class, action, entityId);
    }

    @Test
    void adminInvitesOwnerAndOnlyTheVerifiedPhoneClaimsIt() throws Exception {
        var created = invite(adminToken(), "+88" + phone);
        assertThat(created.status()).isEqualTo(201);
        String invitation = created.data().path("id").asString();
        assertThat(created.data().path("phone").asString()).isEqualTo(phone);
        assertThat(created.data().path("status").asString()).isEqualTo("PENDING");
        assertThat(audits("INVITATION_CREATED", invitation)).isEqualTo(1);

        var duplicate = invite(adminToken(), phone);
        assertThat(duplicate.status()).isEqualTo(200);
        assertThat(duplicate.data().path("id").asString()).isEqualTo(invitation);
        assertThat(audits("INVITATION_CREATED", invitation)).isEqualTo(1);

        var mine = send("GET", "/api/v1/me/building-invitations", recipientToken(), null);
        assertThat(mine.data()).hasSize(1);
        assertThat(mine.data().get(0).path("buildingName").asString()).isEqualTo("Rose Tower");
        assertThat(send("GET", "/api/v1/me/building-invitations", token(recipient, randomPhone()), null).data())
                .isEmpty();
        assertThat(claim(token(UUID.randomUUID(), randomPhone()), invitation, UUID.randomUUID()).code())
                .isEqualTo("INVITATION_NOT_FOUND");
        assertThat(claim(token(recipient, null), invitation, UUID.randomUUID()).status()).isEqualTo(403);

        UUID operation = UUID.randomUUID();
        var claimed = claim(recipientToken(), invitation, operation);
        assertThat(claimed.status()).isEqualTo(200);
        String membership = claimed.data().path("id").asString();
        assertThat(claimed.data().path("role").asString()).isEqualTo("OWNER");
        assertThat(claimed.data().path("userId").asString()).isEqualTo(recipient.toString());
        assertThat(audits("MEMBERSHIP_GRANTED", membership)).isEqualTo(1);
        assertThat(audits("INVITATION_CLAIMED", invitation)).isEqualTo(1);

        assertThat(claim(recipientToken(), invitation, operation).data().path("id").asString()).isEqualTo(membership);
        assertThat(claim(recipientToken(), invitation, UUID.randomUUID()).data().path("id").asString())
                .isEqualTo(membership);
        assertThat(claim(token(UUID.randomUUID(), phone), invitation, UUID.randomUUID()).code())
                .isEqualTo("INVITATION_ALREADY_CLAIMED");
        assertThat(audits("MEMBERSHIP_GRANTED", membership)).isEqualTo(1);
        assertThat(send("GET", "/api/v1/me/building-invitations", recipientToken(), null).data()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_membership WHERE building_id = ?",
                Integer.class, building)).isEqualTo(2);

        var members = send("GET", base() + "/members", adminToken(), null);
        assertThat(members.body().path("meta").path("total").asInt()).isEqualTo(2);
    }

    @Test
    void operationIdReusedForAnotherInvitationConflicts() throws Exception {
        String first = invite(adminToken(), phone).data().path("id").asString();
        UUID otherBuilding = seedBuilding("ACTIVE");
        member(otherBuilding, admin, "BUILDING_ADMIN");
        String second = send("POST", "/api/v1/buildings/" + otherBuilding + "/invitations", adminToken(),
                "{\"phone\":\"" + phone + "\",\"role\":\"OWNER\",\"reason\":\"Second flat\"}").data().path("id")
                .asString();
        UUID operation = UUID.randomUUID();
        assertThat(claim(recipientToken(), first, operation).status()).isEqualTo(200);
        var conflict = claim(recipientToken(), second, operation);
        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.code()).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(claim(recipientToken(), second, UUID.randomUUID()).status()).isEqualTo(200);
    }

    @Test
    void revokedAndExpiredInvitationsCannotBeClaimedAndExpiryAllowsReplacement() throws Exception {
        String revoked = invite(adminToken(), phone).data().path("id").asString();
        assertThat(send("POST", base() + "/invitations/" + revoked + "/revoke", adminToken(), "{\"reason\":\" \"}")
                .status()).isEqualTo(400);
        var revocation = send("POST", base() + "/invitations/" + revoked + "/revoke", adminToken(),
                "{\"reason\":\"Wrong number\"}");
        assertThat(revocation.data().path("status").asString()).isEqualTo("REVOKED");
        assertThat(audits("INVITATION_REVOKED", revoked)).isEqualTo(1);
        assertThat(claim(recipientToken(), revoked, UUID.randomUUID()).code()).isEqualTo("INVITATION_REVOKED");

        String expired = invite(adminToken(), phone).data().path("id").asString();
        assertThat(expired).isNotEqualTo(revoked);
        jdbc.update("UPDATE building_invitation SET created_at = now() - interval '9 days', "
                + "expires_at = now() - interval '2 days' WHERE id = ?::uuid", expired);
        assertThat(claim(recipientToken(), expired, UUID.randomUUID()).code()).isEqualTo("INVITATION_EXPIRED");
        assertThat(send("GET", "/api/v1/me/building-invitations", recipientToken(), null).data()).isEmpty();
        var listed = send("GET", base() + "/invitations", adminToken(), null);
        assertThat(listed.data()).anySatisfy(i -> {
            assertThat(i.path("id").asString()).isEqualTo(expired);
            assertThat(i.path("status").asString()).isEqualTo("EXPIRED");
        });
        assertThat(send("POST", base() + "/invitations/" + expired + "/revoke", adminToken(),
                "{\"reason\":\"Too late\"}").code()).isEqualTo("INVITATION_EXPIRED");

        var replacement = invite(adminToken(), phone);
        assertThat(replacement.status()).isEqualTo(201);
        assertThat(jdbc.queryForObject("SELECT status FROM building_invitation WHERE id = ?::uuid", String.class,
                expired)).isEqualTo("EXPIRED");
        assertThat(claim(recipientToken(), replacement.data().path("id").asString(), UUID.randomUUID()).status())
                .isEqualTo(200);
    }

    @Test
    void accessIsCheckedPerBuildingAndRole() throws Exception {
        UUID owner = UUID.randomUUID();
        member(building, owner, "OWNER");
        assertThat(send("GET", base() + "/invitations", token(owner, null), null).status()).isEqualTo(403);
        assertThat(invite(token(owner, null), phone).status()).isEqualTo(403);

        var stranger = send("GET", base() + "/members", token(UUID.randomUUID(), null), null);
        assertThat(stranger.status()).isEqualTo(404);
        assertThat(stranger.code()).isEqualTo("BUILDING_NOT_FOUND");
        assertThat(send("GET", "/api/v1/buildings/" + UUID.randomUUID() + "/members", adminToken(), null).status())
                .isEqualTo(404);

        UUID otherBuilding = seedBuilding("ACTIVE");
        UUID otherAdmin = UUID.randomUUID();
        member(otherBuilding, otherAdmin, "BUILDING_ADMIN");
        String foreign = send("POST", "/api/v1/buildings/" + otherBuilding + "/invitations",
                token(otherAdmin, null), "{\"phone\":\"" + phone + "\",\"role\":\"OWNER\",\"reason\":\"Owner\"}")
                .data().path("id").asString();
        assertThat(send("POST", base() + "/invitations/" + foreign + "/revoke", adminToken(),
                "{\"reason\":\"Substituted id\"}").code()).isEqualTo("INVITATION_NOT_FOUND");
        assertThat(send("GET", "/api/v1/buildings/" + otherBuilding + "/invitations", adminToken(), null).status())
                .isEqualTo(404);

        assertThat(send("GET", base() + "/members", token(UUID.randomUUID(), null, "PLATFORM_ADMIN"), null)
                .status()).isEqualTo(200);
        assertThat(send("POST", base() + "/invitations", adminToken(),
                "{\"phone\":\"" + phone + "\",\"role\":\"BUILDING_ADMIN\",\"reason\":\"Staff\"}").status())
                .isEqualTo(400);
        assertThat(invite(adminToken(), "12345").status()).isEqualTo(400);
    }

    @Test
    void revokingOwnerMembershipIsVersionedAndNeedsANewInvitationToReinstate() throws Exception {
        String invitation = invite(adminToken(), phone).data().path("id").asString();
        var membership = claim(recipientToken(), invitation, UUID.randomUUID()).data();
        String id = membership.path("id").asString();
        long version = membership.path("version").asLong();

        assertThat(send("POST", base() + "/members/" + id + "/revoke", adminToken(),
                "{\"reason\":\"Sold flat\",\"expectedVersion\":" + (version + 5) + "}").code())
                .isEqualTo("STALE_VERSION");
        assertThat(send("POST", base() + "/members/" + adminMembership + "/revoke", adminToken(),
                "{\"reason\":\"Remove admin\",\"expectedVersion\":0}").code()).isEqualTo("MEMBERSHIP_NOT_REVOCABLE");
        var revoked = send("POST", base() + "/members/" + id + "/revoke", adminToken(),
                "{\"reason\":\"Sold flat\",\"expectedVersion\":" + version + "}");
        assertThat(revoked.status()).isEqualTo(200);
        assertThat(revoked.data().path("status").asString()).isEqualTo("REVOKED");
        assertThat(audits("MEMBERSHIP_REVOKED", id)).isEqualTo(1);
        assertThat(send("POST", base() + "/members/" + id + "/revoke", adminToken(),
                "{\"reason\":\"Again\",\"expectedVersion\":" + (version + 1) + "}").code())
                .isEqualTo("MEMBERSHIP_NOT_ACTIVE");

        assertThat(claim(recipientToken(), invitation, UUID.randomUUID()).code())
                .isEqualTo("INVITATION_ALREADY_CLAIMED");
        String again = invite(adminToken(), phone).data().path("id").asString();
        var reinstated = claim(recipientToken(), again, UUID.randomUUID()).data();
        assertThat(reinstated.path("id").asString()).isEqualTo(id);
        assertThat(reinstated.path("status").asString()).isEqualTo("ACTIVE");
        assertThat(audits("MEMBERSHIP_REINSTATED", id)).isEqualTo(1);
    }

    @Test
    void suspendedBuildingsAreReadOnly() throws Exception {
        String invitation = invite(adminToken(), phone).data().path("id").asString();
        jdbc.update("UPDATE building SET status = 'SUSPENDED' WHERE id = ?", building);
        assertThat(invite(adminToken(), randomPhone()).code()).isEqualTo("BUILDING_READ_ONLY");
        assertThat(claim(recipientToken(), invitation, UUID.randomUUID()).code()).isEqualTo("BUILDING_READ_ONLY");
        assertThat(send("GET", base() + "/invitations", adminToken(), null).status()).isEqualTo(200);
    }

    @Test
    void concurrentInvitesForOnePhoneCreateOneInvitation() throws Exception {
        int callers = 6;
        var barrier = new CyclicBarrier(callers);
        var pool = Executors.newFixedThreadPool(callers);
        try {
            List<Future<Reply>> replies = new ArrayList<>();
            for (int i = 0; i < callers; i++) {
                replies.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return invite(adminToken(), phone);
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (var reply : replies) {
                var r = reply.get(30, TimeUnit.SECONDS);
                statuses.add(r.status());
                ids.add(r.data().path("id").asString());
            }
            assertThat(statuses).containsOnly(200, 201).containsOnlyOnce(201);
            assertThat(ids).containsOnly(ids.get(0));
        } finally {
            pool.shutdownNow();
        }
    }
}
