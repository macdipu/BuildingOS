package com.buildingos.identity.auth;

import com.buildingos.identity.auth.application.port.in.VerifyOtpChallenge.Result;
import com.buildingos.identity.auth.application.port.out.OtpChallengeRepository;
import com.buildingos.identity.auth.application.port.out.TokenIssuer;
import com.buildingos.identity.auth.application.port.out.UserRepository;
import com.buildingos.identity.auth.application.usecase.StartOtpChallengeService;
import com.buildingos.identity.auth.application.usecase.VerifyOtpChallengeService;
import com.buildingos.identity.auth.domain.OtpChallenge;
import com.buildingos.identity.auth.domain.PlatformRole;
import com.buildingos.identity.auth.domain.User;
import java.time.Clock;
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
        @Override public OtpChallenge start(String phone, Instant startedAt, Instant expiresAt) {
            var challenge = new OtpChallenge(UUID.randomUUID(), phone, startedAt, expiresAt, 0, null);
            store.put(challenge.id(), challenge);
            return challenge;
        }
        @Override public Optional<OtpChallenge> find(UUID attemptId) { return Optional.ofNullable(store.get(attemptId)); }
        @Override public OtpChallenge recordAttempt(UUID attemptId) {
            var c = store.get(attemptId);
            var updated = new OtpChallenge(c.id(), c.phone(), c.createdAt(), c.expiresAt(), c.attemptCount() + 1, c.consumedAt());
            store.put(attemptId, updated);
            return updated;
        }
        @Override public boolean consume(UUID attemptId, Instant consumedAt) {
            var c = store.get(attemptId);
            if (c.isConsumed()) return false;
            store.put(attemptId, new OtpChallenge(c.id(), c.phone(), c.createdAt(), c.expiresAt(), c.attemptCount(), consumedAt));
            return true;
        }
    };

    private final Map<String, User> users = new HashMap<>();
    private final UserRepository userRepository = new UserRepository() {
        @Override public Optional<User> findByPhone(String phone) { return Optional.ofNullable(users.get(phone)); }
        @Override public User findOrCreateByPhone(String phone) {
            return users.computeIfAbsent(phone, p -> new User(UUID.randomUUID(), p, now.get(), Set.of()));
        }
    };

    private final TokenIssuer tokenIssuer = (userId, phone, roles) -> new TokenIssuer.IssuedToken("fake-token-" + userId, 900);

    private final StartOtpChallengeService startService = new StartOtpChallengeService(repository, clock);
    private final VerifyOtpChallengeService verifyService =
            new VerifyOtpChallengeService(repository, (phone, code) -> "000000".equals(code), userRepository, tokenIssuer, clock);

    @Test
    void correctCodeVerifiesAndIssuesSession() {
        var started = startService.execute(PHONE);
        var result = verifyService.execute(started.attemptId(), PHONE, "000000");
        assertThat(result).isInstanceOf(Result.Verified.class);
        var verified = (Result.Verified) result;
        assertThat(verified.phone()).isEqualTo(PHONE);
        assertThat(verified.accessToken()).startsWith("fake-token-");
    }

    @Test
    void wrongCodeIsRejected() {
        var started = startService.execute(PHONE);
        var result = verifyService.execute(started.attemptId(), PHONE, "123456");
        assertThat(result).isEqualTo(new Result.Rejected(Result.Reason.INVALID_CODE));
    }

    @Test
    void unknownAttemptIsRejected() {
        var result = verifyService.execute(UUID.randomUUID(), PHONE, "000000");
        assertThat(result).isEqualTo(new Result.Rejected(Result.Reason.UNKNOWN_ATTEMPT));
    }

    @Test
    void phoneMismatchIsRejected() {
        var started = startService.execute(PHONE);
        var result = verifyService.execute(started.attemptId(), "+8801700000001", "000000");
        assertThat(result).isEqualTo(new Result.Rejected(Result.Reason.PHONE_MISMATCH));
    }

    @Test
    void expiredChallengeIsRejected() {
        var started = startService.execute(PHONE);
        now.set(now.get().plus(StartOtpChallengeService.CHALLENGE_TTL).plusSeconds(1));
        var result = verifyService.execute(started.attemptId(), PHONE, "000000");
        assertThat(result).isEqualTo(new Result.Rejected(Result.Reason.EXPIRED));
    }

    @Test
    void consumedChallengeCannotBeReused() {
        var started = startService.execute(PHONE);
        verifyService.execute(started.attemptId(), PHONE, "000000");
        var second = verifyService.execute(started.attemptId(), PHONE, "000000");
        assertThat(second).isEqualTo(new Result.Rejected(Result.Reason.ALREADY_CONSUMED));
    }

    @Test
    void attemptsAreExhaustedAfterRepeatedWrongCodes() {
        var started = startService.execute(PHONE);
        Result last = null;
        for (int i = 0; i < OtpChallenge.MAX_ATTEMPTS + 1; i++) {
            last = verifyService.execute(started.attemptId(), PHONE, "999999");
        }
        assertThat(last).isEqualTo(new Result.Rejected(Result.Reason.ATTEMPTS_EXHAUSTED));
    }

    @Test
    void rejectsMalformedPhoneNumberAtStart() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> startService.execute("not-a-phone"));
    }

    @Test
    void treatsCodeAsStringPreservingLeadingZeros() {
        var started = startService.execute(PHONE);
        var result = verifyService.execute(started.attemptId(), PHONE, "000000");
        assertThat(result).isInstanceOf(Result.Verified.class);
    }
}
