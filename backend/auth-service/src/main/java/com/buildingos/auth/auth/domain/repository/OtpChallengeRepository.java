package com.buildingos.auth.auth.domain.repository;

import com.buildingos.auth.auth.domain.model.OtpChallenge;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository {
    /** Atomically checks per-phone limits (canonical phone) and inserts; empty means rate limited. */
    Optional<OtpChallenge> start(String phone, Instant now, Instant expiresAt, Duration cooldown, int maxPerHour);
    void saveCodeHash(UUID attemptId, String codeHash);
    Optional<OtpChallenge> find(UUID attemptId);
    /** Atomically increments and returns the post-increment state. */
    OtpChallenge recordAttempt(UUID attemptId);
    /** Consumes only a still-valid, unconsumed challenge. */
    boolean consume(UUID attemptId, Instant now);
}
