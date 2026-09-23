package com.buildingos.account.auth.domain.repository;

import com.buildingos.account.auth.domain.model.OtpChallenge;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository {
    OtpChallenge start(String phone, Instant now, Instant expiresAt);

    Optional<OtpChallenge> find(UUID attemptId);

    /** Atomically increments attempt_count and returns the post-increment challenge. */
    OtpChallenge recordAttempt(UUID attemptId);

    /** Atomically sets consumed_at only if not already consumed; returns true if this call consumed it. */
    boolean consume(UUID attemptId, Instant now);
}
