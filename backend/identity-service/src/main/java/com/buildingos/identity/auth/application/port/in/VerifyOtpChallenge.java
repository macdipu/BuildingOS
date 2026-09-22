package com.buildingos.identity.auth.application.port.in;

import com.buildingos.identity.auth.domain.PlatformRole;
import java.util.Set;
import java.util.UUID;

public interface VerifyOtpChallenge {
    Result execute(UUID attemptId, String phone, String code);

    sealed interface Result permits Result.Verified, Result.Rejected {
        record Verified(UUID userId, String phone, Set<PlatformRole> platformRoles, String accessToken,
                long expiresInSeconds) implements Result {}

        record Rejected(Reason reason) implements Result {}

        enum Reason { UNKNOWN_ATTEMPT, PHONE_MISMATCH, EXPIRED, ALREADY_CONSUMED, ATTEMPTS_EXHAUSTED, INVALID_CODE }
    }
}
