package com.buildingos.account.auth;

import com.buildingos.account.auth.application.port.out.TokenIssuer;
import com.buildingos.account.auth.application.startotp.StartOtpCommand;
import com.buildingos.account.auth.application.startotp.StartOtpService;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpCommand;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpResult;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpService;
import com.buildingos.account.auth.domain.model.OtpChallenge;
import com.buildingos.account.auth.domain.model.PlatformRole;
import com.buildingos.account.auth.domain.model.User;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import com.buildingos.account.auth.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import com.buildingos.account.auth.infrastructure.security.DevelopmentOtpProvider;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the OTP challenge lifecycle contract recorded in OTP-PROVIDER.md, using in-memory fakes. */
class OtpChallengeFlowTest {
    private static final String PHONE = "+8801700000000";

    private final Map<UUID, OtpChallenge> store = new HashMap<>();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { throw new UnsupportedOperationException(); }
        @Override public Instant instant() { return now.get(); }
    };

    private final OtpChallengeRepository repository = new OtpChallengeRepository() {
        @Override public Optional<OtpChallenge> start(String phone, Instant startedAt, Instant expiresAt, Duration cooldown, int maxPerHour) {
            var challenge = new OtpChallenge(UUID.randomUUID(), phone, startedAt, expiresAt, 0, null, null);
            store.put(challenge.id(), challenge);
            return Optional.of(challenge);
        }
        @Override public void saveCodeHash(UUID id, String hash) { throw new UnsupportedOperationException(); }
        @Override public Optional<OtpChallenge> find(UUID attemptId) { return Optional.ofNullable(store.get(attemptId)); }
        @Override public OtpChallenge recordAttempt(UUID attemptId) {
            var c = store.get(attemptId);
            var updated = new OtpChallenge(c.id(), c.phone(), c.createdAt(), c.expiresAt(), c.attemptCount() + 1, c.consumedAt(), c.codeHash());
            store.put(attemptId, updated);
            return updated;
        }
        @Override public boolean consume(UUID attemptId, Instant consumedAt) {
            var c = store.get(attemptId);
            if (c.isConsumed()) return false;
            store.put(attemptId, new OtpChallenge(c.id(), c.phone(), c.createdAt(), c.expiresAt(), c.attemptCount(), consumedAt, c.codeHash()));
            return true;
        }
    };

    private final Map<String, User> users = new HashMap<>();
    private final UserRepository userRepository = new UserRepository() {
        @Override public Optional<User> findByPhone(String phone) { return Optional.ofNullable(users.get(phone)); }
        @Override public User findOrCreateByPhone(String phone) {
            return users.computeIfAbsent(phone, p -> new User(UUID.randomUUID(), p, now.get(), Set.of()));
        }
        @Override public void grantPlatformRole(UUID userId, PlatformRole role) {
            throw new UnsupportedOperationException();
        }
    };

    private final TokenIssuer tokenIssuer = (userId, phone, roles) -> new TokenIssuer.IssuedToken("fake-token-" + userId, 900);

    private final StartOtpService startService = new StartOtpService(repository, new DevelopmentOtpProvider(), clock, Duration.ofSeconds(60), 5);
    private final VerifyOtpService verifyService =
            new VerifyOtpService(repository, new DevelopmentOtpProvider(), userRepository, tokenIssuer, clock);

    @Test
    void correctCodeVerifiesAndIssuesSession() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        var result = verifyService.execute(new VerifyOtpCommand(started.attemptId(), PHONE, "000000"));
        assertThat(result).isInstanceOf(VerifyOtpResult.Verified.class);
        var verified = (VerifyOtpResult.Verified) result;
        assertThat(verified.phone()).isEqualTo("01700000000");
        assertThat(verified.accessToken()).startsWith("fake-token-");
    }

    @Test
    void wrongCodeIsRejected() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        var result = verifyService.execute(new VerifyOtpCommand(started.attemptId(), PHONE, "123456"));
        assertThat(result).isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.INVALID_CODE));
    }

    @Test
    void unknownAttemptIsRejected() {
        var result = verifyService.execute(new VerifyOtpCommand(UUID.randomUUID(), PHONE, "000000"));
        assertThat(result).isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.UNKNOWN_ATTEMPT));
    }

    @Test
    void anyAcceptedPhoneFormVerifiesTheSameCanonicalUser() {
        var started = startService.execute(new StartOtpCommand("01700000000"));
        var result = verifyService.execute(new VerifyOtpCommand(started.attemptId(), "8801700000000", "000000"));
        assertThat(result).isInstanceOf(VerifyOtpResult.Verified.class);
        assertThat(((VerifyOtpResult.Verified) result).phone()).isEqualTo("01700000000");
    }

    @Test
    void unparseablePhoneAtVerifyIsAMismatch() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        var result = verifyService.execute(new VerifyOtpCommand(started.attemptId(), "not-a-phone", "000000"));
        assertThat(result).isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.PHONE_MISMATCH));
    }

    @Test
    void phoneMismatchIsRejected() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        var result = verifyService.execute(new VerifyOtpCommand(started.attemptId(), "+8801700000001", "000000"));
        assertThat(result).isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.PHONE_MISMATCH));
    }

    @Test
    void expiredChallengeIsRejected() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        now.set(now.get().plus(StartOtpService.CHALLENGE_TTL));
        var result = verifyService.execute(new VerifyOtpCommand(started.attemptId(), PHONE, "000000"));
        assertThat(result).isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.EXPIRED));
    }

    @Test
    void consumedChallengeCannotBeReused() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        verifyService.execute(new VerifyOtpCommand(started.attemptId(), PHONE, "000000"));
        var second = verifyService.execute(new VerifyOtpCommand(started.attemptId(), PHONE, "000000"));
        assertThat(second).isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.ALREADY_CONSUMED));
    }

    @Test
    void attemptsAreExhaustedAfterRepeatedWrongCodes() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        VerifyOtpResult last = null;
        for (int i = 0; i < OtpChallenge.MAX_ATTEMPTS + 1; i++) {
            last = verifyService.execute(new VerifyOtpCommand(started.attemptId(), PHONE, "999999"));
        }
        assertThat(last).isEqualTo(new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.ATTEMPTS_EXHAUSTED));
    }

    @Test
    void rejectsMalformedPhoneNumberAtStart() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> startService.execute(new StartOtpCommand("not-a-phone")));
    }

    @Test
    void treatsCodeAsStringPreservingLeadingZeros() {
        var started = startService.execute(new StartOtpCommand(PHONE));
        var result = verifyService.execute(new VerifyOtpCommand(started.attemptId(), PHONE, "000000"));
        assertThat(result).isInstanceOf(VerifyOtpResult.Verified.class);
    }
}
