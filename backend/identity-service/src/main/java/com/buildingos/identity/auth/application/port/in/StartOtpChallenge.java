package com.buildingos.identity.auth.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface StartOtpChallenge {
    Result execute(String phone);

    record Result(UUID attemptId, Instant expiresAt) {}
}
