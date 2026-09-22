package com.buildingos.identity.auth.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Server-held OTP challenge state (attempt id, phone binding, expiry, attempt count,
 * consumption). Mirrors the contract recorded in the superseded BOS-002 OTP-PROVIDER.md.
 */
public record OtpChallenge(UUID id, String phone, Instant createdAt, Instant expiresAt,
        int attemptCount, Instant consumedAt) {

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean attemptsExhausted() {
        return attemptCount >= MAX_ATTEMPTS;
    }

    public static final int MAX_ATTEMPTS = 5;
}
