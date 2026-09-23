package com.buildingos.account.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OtpChallenge(UUID id, String phone, Instant createdAt, Instant expiresAt,
        int attemptCount, Instant consumedAt, String codeHash) {
    public static final int MAX_ATTEMPTS = 5;

    public boolean isExpired(Instant now) { return !now.isBefore(expiresAt); }
    public boolean isConsumed() { return consumedAt != null; }
    public boolean attemptsExhausted() { return attemptCount >= MAX_ATTEMPTS; }
}
