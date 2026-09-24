package com.buildingos.auth.auth.presentation.rest.mapper;

import com.buildingos.auth.auth.application.startotp.StartOtpCommand;
import com.buildingos.auth.auth.application.startotp.StartOtpResult;
import com.buildingos.auth.auth.application.verifyotp.VerifyOtpCommand;
import com.buildingos.auth.auth.application.verifyotp.VerifyOtpResult;
import com.buildingos.auth.auth.presentation.rest.request.StartOtpRequest;
import com.buildingos.auth.auth.presentation.rest.request.VerifyOtpRequest;
import com.buildingos.auth.auth.presentation.rest.response.StartOtpResponse;
import com.buildingos.auth.auth.presentation.rest.response.VerifiedUserResponse;
import com.buildingos.auth.auth.presentation.rest.response.VerifyOtpResponse;
import com.buildingos.platform.web.response.ApiError;

/** Translates between the OTP login HTTP contract and the application use cases. */
public final class AuthApiMapper {
    private AuthApiMapper() {
    }

    public static StartOtpCommand toCommand(StartOtpRequest request) {
        return new StartOtpCommand(request.phone());
    }

    public static VerifyOtpCommand toCommand(VerifyOtpRequest request) {
        return new VerifyOtpCommand(request.attemptId(), request.phone(), request.code());
    }

    public static StartOtpResponse toResponse(StartOtpResult result) {
        return new StartOtpResponse(result.attemptId(), result.expiresAt().toString());
    }

    public static VerifyOtpResponse toResponse(VerifyOtpResult.Verified verified) {
        return new VerifyOtpResponse(verified.accessToken(), "Bearer", verified.expiresInSeconds(),
                new VerifiedUserResponse(verified.userId(), verified.phone(),
                        verified.platformRoles().stream().map(Enum::name).toList()));
    }

    public static ApiError toError(VerifyOtpResult.Rejected rejected, String traceId) {
        return ApiError.of("OTP_" + rejected.reason().name(), "OTP verification failed", traceId);
    }
}
