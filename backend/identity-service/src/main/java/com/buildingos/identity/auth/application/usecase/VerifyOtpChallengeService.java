package com.buildingos.identity.auth.application.usecase;

import com.buildingos.identity.auth.application.port.in.VerifyOtpChallenge;
import com.buildingos.identity.auth.application.port.out.OtpChallengeRepository;
import com.buildingos.identity.auth.application.port.out.OtpCodeVerifier;
import com.buildingos.identity.auth.application.port.out.TokenIssuer;
import com.buildingos.identity.auth.application.port.out.UserRepository;
import com.buildingos.identity.auth.domain.OtpChallenge;
import java.time.Clock;
import java.util.UUID;

public final class VerifyOtpChallengeService implements VerifyOtpChallenge {
    private final OtpChallengeRepository challenges;
    private final OtpCodeVerifier codeVerifier;
    private final UserRepository users;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public VerifyOtpChallengeService(OtpChallengeRepository challenges, OtpCodeVerifier codeVerifier,
            UserRepository users, TokenIssuer tokenIssuer, Clock clock) {
        this.challenges = challenges;
        this.codeVerifier = codeVerifier;
        this.users = users;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Override
    public Result execute(UUID attemptId, String phone, String code) {
        var found = challenges.find(attemptId);
        if (found.isEmpty()) {
            return new Result.Rejected(Result.Reason.UNKNOWN_ATTEMPT);
        }
        OtpChallenge challenge = found.get();
        if (!challenge.phone().equals(phone)) {
            return new Result.Rejected(Result.Reason.PHONE_MISMATCH);
        }
        if (challenge.isConsumed()) {
            return new Result.Rejected(Result.Reason.ALREADY_CONSUMED);
        }
        if (challenge.isExpired(clock.instant())) {
            return new Result.Rejected(Result.Reason.EXPIRED);
        }
        if (challenge.attemptsExhausted()) {
            return new Result.Rejected(Result.Reason.ATTEMPTS_EXHAUSTED);
        }
        // Atomic increment-then-check guards concurrent verifies racing past the pre-check above.
        challenge = challenges.recordAttempt(attemptId);
        if (challenge.attemptCount() > OtpChallenge.MAX_ATTEMPTS) {
            return new Result.Rejected(Result.Reason.ATTEMPTS_EXHAUSTED);
        }
        if (!codeVerifier.isValidCode(phone, code)) {
            return new Result.Rejected(Result.Reason.INVALID_CODE);
        }
        boolean consumedNow = challenges.consume(attemptId, clock.instant());
        if (!consumedNow) {
            return new Result.Rejected(Result.Reason.ALREADY_CONSUMED);
        }
        var user = users.findOrCreateByPhone(phone);
        var issued = tokenIssuer.issue(user.id(), user.phone(), user.platformRoles());
        return new Result.Verified(user.id(), user.phone(), user.platformRoles(), issued.accessToken(),
                issued.expiresInSeconds());
    }
}
