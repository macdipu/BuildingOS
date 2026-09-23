package com.buildingos.account.auth.application.verifyotp;

import com.buildingos.account.auth.application.port.out.OtpProvider;
import com.buildingos.account.auth.application.port.out.TokenIssuer;
import com.buildingos.account.auth.domain.model.OtpChallenge;
import com.buildingos.account.auth.domain.model.PhoneNumber;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import com.buildingos.account.auth.domain.repository.UserRepository;
import java.time.Clock;

public final class VerifyOtpService implements VerifyOtpUseCase {
    private final OtpChallengeRepository challenges;
    private final OtpProvider codeVerifier;
    private final UserRepository users;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public VerifyOtpService(OtpChallengeRepository challenges, OtpProvider codeVerifier,
            UserRepository users, TokenIssuer tokenIssuer, Clock clock) {
        this.challenges = challenges;
        this.codeVerifier = codeVerifier;
        this.users = users;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Override
    public VerifyOtpResult execute(VerifyOtpCommand command) {
        var attemptId = command.attemptId();
        var code = command.code();
        var found = challenges.find(attemptId);
        if (found.isEmpty()) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.UNKNOWN_ATTEMPT);
        }
        OtpChallenge challenge = found.get();
        String phone;
        try {
            phone = PhoneNumber.parse(command.phone()).value();
        } catch (IllegalArgumentException invalid) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.PHONE_MISMATCH);
        }
        if (!challenge.phone().equals(phone)) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.PHONE_MISMATCH);
        }
        if (challenge.isConsumed()) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.ALREADY_CONSUMED);
        }
        if (challenge.isExpired(clock.instant())) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.EXPIRED);
        }
        if (challenge.attemptsExhausted()) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.ATTEMPTS_EXHAUSTED);
        }
        // Atomic increment-then-check guards concurrent verifies racing past the pre-check above.
        challenge = challenges.recordAttempt(attemptId);
        if (challenge.attemptCount() > OtpChallenge.MAX_ATTEMPTS) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.ATTEMPTS_EXHAUSTED);
        }
        if (!codeVerifier.verify(challenge, code)) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.INVALID_CODE);
        }
        boolean consumedNow = challenges.consume(attemptId, clock.instant());
        if (!consumedNow) {
            return new VerifyOtpResult.Rejected(VerifyOtpResult.Reason.ALREADY_CONSUMED);
        }
        var user = users.findOrCreateByPhone(phone);
        var issued = tokenIssuer.issue(user.id(), user.phone(), user.platformRoles());
        return new VerifyOtpResult.Verified(user.id(), user.phone(), user.platformRoles(), issued.accessToken(),
                issued.expiresInSeconds());
    }
}
