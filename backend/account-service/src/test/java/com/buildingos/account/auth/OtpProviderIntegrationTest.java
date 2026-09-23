package com.buildingos.account.auth;

import com.buildingos.account.auth.application.port.out.*;
import com.buildingos.account.auth.application.startotp.*;
import com.buildingos.account.auth.application.verifyotp.*;
import com.buildingos.account.auth.domain.model.*;
import com.buildingos.account.auth.domain.repository.UserRepository;
import com.buildingos.account.auth.infrastructure.persistence.repository.JdbcOtpChallengeRepositoryAdapter;
import com.buildingos.account.auth.infrastructure.security.HashedCodeOtpProvider;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
class OtpProviderIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24")
                    .asCompatibleSubstituteFor("postgres"));
    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    static final String PHONE = "+8801700000000";
    JdbcTemplate jdbc;
    JdbcOtpChallengeRepositoryAdapter repository;
    RecordingSmsSender sender;
    HashedCodeOtpProvider provider;
    final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    static final class RecordingSmsSender implements SmsSender {
        final List<String> messages = new ArrayList<>();
        @Override public void send(String phone, String message) {
            assertThat(phone).isEqualTo(PHONE);
            messages.add(message);
        }
        String code() { return messages.get(0).substring(messages.get(0).length() - 6); }
    }

    @BeforeEach void setup() {
        var datasource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(datasource).load().migrate();
        jdbc = new JdbcTemplate(datasource);
        jdbc.update("DELETE FROM otp_challenge");
        repository = new JdbcOtpChallengeRepositoryAdapter(jdbc, new DataSourceTransactionManager(datasource));
        sender = new RecordingSmsSender();
        provider = new HashedCodeOtpProvider(repository, sender, "test-only-pepper-01234567890123456789");
    }

    StartOtpService start(OtpProvider selected) {
        return new StartOtpService(repository, selected, clock, Duration.ofSeconds(60), 5);
    }

    @Test void storesOnlyHashSendsOnceAndVerifiesOnceThroughUseCase() {
        var attempt = start(provider).execute(new StartOtpCommand(PHONE));
        var challenge = repository.find(attempt.attemptId()).orElseThrow();
        assertThat(sender.messages).hasSize(1);
        assertThat(sender.code()).matches("[0-9]{6}");
        assertThat(challenge.codeHash()).matches("[0-9a-f]{64}").isNotEqualTo(sender.code());
        assertThat(provider.verify(challenge, "not-a-code")).isFalse();
        String wrong = sender.code().equals("000000") ? "111111" : "000000";
        var users = mock(UserRepository.class);
        when(users.findOrCreateByPhone(PHONE)).thenReturn(new User(UUID.randomUUID(), PHONE, NOW, Set.of()));
        TokenIssuer tokens = (id, phone, roles) -> new TokenIssuer.IssuedToken("test-token", 900);
        var verify = new VerifyOtpService(repository, provider, users, tokens, clock);
        assertThat(verify.execute(new VerifyOtpCommand(challenge.id(), PHONE, wrong)))
                .isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.INVALID_CODE));
        assertThat(verify.execute(new VerifyOtpCommand(challenge.id(), PHONE, sender.code())))
                .isInstanceOf(VerifyOtpResult.Verified.class);
        assertThat(verify.execute(new VerifyOtpCommand(challenge.id(), PHONE, sender.code())))
                .isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.ALREADY_CONSUMED));
        assertThat(sender.messages).hasSize(1);
    }

    @Test void hashIsBoundToChallengeAndCannotBeTransplanted() {
        var first = start(provider).execute(new StartOtpCommand(PHONE));
        var original = repository.find(first.attemptId()).orElseThrow();
        var other = new OtpChallenge(UUID.randomUUID(), PHONE, NOW, NOW.plusSeconds(300), 0, null, original.codeHash());
        assertThat(provider.verify(other, sender.code())).isFalse();
        assertThatThrownBy(() -> provider.issue(original)).isInstanceOf(IllegalStateException.class);
        assertThat(sender.messages).hasSize(1);
    }

    @Test void cooldownAndRollingHourHaveExactBoundaries() {
        assertThat(repository.start(PHONE, NOW, NOW.plusSeconds(300), Duration.ofSeconds(60), 5)).isPresent();
        assertThat(repository.start(PHONE, NOW.plusSeconds(59), NOW.plusSeconds(359), Duration.ofSeconds(60), 5)).isEmpty();
        for (int i = 1; i < 5; i++) {
            Instant t = NOW.plusSeconds(i * 60);
            assertThat(repository.start(PHONE, t, t.plusSeconds(300), Duration.ofSeconds(60), 5)).isPresent();
        }
        Instant limited = NOW.plusSeconds(300);
        assertThat(repository.start(PHONE, limited, limited.plusSeconds(300), Duration.ofSeconds(60), 5)).isEmpty();
        Instant boundary = NOW.plusSeconds(3600);
        assertThat(repository.start(PHONE, boundary, boundary.plusSeconds(300), Duration.ofSeconds(60), 5)).isPresent();
    }

    @Test void concurrentStartsAcrossConnectionsCannotBypassLimits() throws Exception {
        var pool = Executors.newFixedThreadPool(8);
        var barrier = new CyclicBarrier(8);
        try {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 8; i++) {
                final String phone = i % 2 == 0 ? PHONE : PHONE.substring(1);
                futures.add(pool.submit(() -> {
                    barrier.await(10, TimeUnit.SECONDS);
                    return repository.start(phone, NOW, NOW.plusSeconds(300), Duration.ofSeconds(60), 5).isPresent();
                }));
            }
            int accepted = 0;
            for (var future : futures) if (future.get(15, TimeUnit.SECONDS)) accepted++;
            assertThat(accepted).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM otp_challenge", Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void consumeRejectsExactExpiryAndConcurrentReplay() throws Exception {
        var c = repository.start(PHONE, NOW, NOW.plusSeconds(300), Duration.ofSeconds(60), 5).orElseThrow();
        assertThat(c.isExpired(NOW.plusSeconds(300))).isTrue();
        assertThat(repository.consume(c.id(), NOW.plusSeconds(300))).isFalse();
        var pool = Executors.newFixedThreadPool(2);
        try {
            var one = pool.submit(() -> repository.consume(c.id(), NOW.plusSeconds(1)));
            var two = pool.submit(() -> repository.consume(c.id(), NOW.plusSeconds(1)));
            assertThat(List.of(one.get(), two.get())).containsExactlyInAnyOrder(true, false);
        } finally { pool.shutdownNow(); }
    }

    @Test void senderFailureInvalidatesChallengeAndDoesNotExposeMessageOrRetry() {
        var failing = new HashedCodeOtpProvider(repository, (phone, message) -> {
            throw new IllegalStateException("vendor secret " + message);
        }, "test-only-pepper-01234567890123456789");
        assertThatThrownBy(() -> start(failing).execute(new StartOtpCommand(PHONE)))
                .isInstanceOf(IllegalStateException.class).hasMessage("OTP delivery failed").hasNoCause();
        UUID id = jdbc.queryForObject("SELECT id FROM otp_challenge", UUID.class);
        assertThat(repository.find(id).orElseThrow().isConsumed()).isTrue();
        assertThatThrownBy(() -> start(failing).execute(new StartOtpCommand(PHONE)))
                .isInstanceOf(OtpRateLimitedException.class);
    }
}
