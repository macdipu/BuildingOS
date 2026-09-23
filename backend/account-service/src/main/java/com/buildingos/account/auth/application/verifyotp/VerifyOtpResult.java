package com.buildingos.account.auth.application.verifyotp;

import com.buildingos.account.auth.domain.model.PlatformRole;
import java.util.Set;
import java.util.UUID;

public sealed interface VerifyOtpResult permits VerifyOtpResult.Verified, VerifyOtpResult.Rejected {
    record Verified(UUID userId, String phone, Set<PlatformRole> platformRoles, String accessToken,
            long expiresInSeconds) implements VerifyOtpResult {}

    record Rejected(Reason reason) implements VerifyOtpResult {}

    enum Reason { UNKNOWN_ATTEMPT, PHONE_MISMATCH, EXPIRED, ALREADY_CONSUMED, ATTEMPTS_EXHAUSTED, INVALID_CODE }
}
