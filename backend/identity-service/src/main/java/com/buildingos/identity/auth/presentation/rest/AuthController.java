package com.buildingos.identity.auth.presentation.rest;

import com.buildingos.identity.auth.application.port.in.StartOtpChallenge;
import com.buildingos.identity.auth.application.port.in.VerifyOtpChallenge;
import com.buildingos.platform.web.ApiError;
import com.buildingos.platform.web.ApiEnvelope;
import com.buildingos.platform.web.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local/test-only phone+OTP login. Never active outside local/test — see AuthConfiguration. */
@RestController
@RequestMapping("/api/v1/auth/otp")
@Profile({"local", "test"})
public class AuthController {
    private final StartOtpChallenge start;
    private final VerifyOtpChallenge verify;

    public AuthController(StartOtpChallenge start, VerifyOtpChallenge verify) {
        this.start = start;
        this.verify = verify;
    }

    @PostMapping("/start")
    public ApiEnvelope<StartResponse> start(@Valid @RequestBody StartRequest request, HttpServletRequest httpRequest) {
        var result = start.execute(request.phone());
        return ApiEnvelope.of(new StartResponse(result.attemptId(), result.expiresAt().toString()),
                CorrelationFilter.traceId(httpRequest));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyRequest request, HttpServletRequest httpRequest) {
        String traceId = CorrelationFilter.traceId(httpRequest);
        var result = verify.execute(request.attemptId(), request.phone(), request.code());
        if (result instanceof VerifyOtpChallenge.Result.Verified verified) {
            return ResponseEntity.ok(ApiEnvelope.of(new VerifyResponse(
                    verified.accessToken(), "Bearer", verified.expiresInSeconds(),
                    new VerifiedUser(verified.userId(), verified.phone(),
                            verified.platformRoles().stream().map(Enum::name).toList())), traceId));
        }
        var rejected = (VerifyOtpChallenge.Result.Rejected) result;
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("OTP_" + rejected.reason().name(), "OTP verification failed", traceId));
    }

    public record StartRequest(@NotBlank String phone) {}
    public record StartResponse(UUID attemptId, String expiresAt) {}
    public record VerifyRequest(@NotNull UUID attemptId, @NotBlank String phone, @NotBlank String code) {}
    public record VerifyResponse(String accessToken, String tokenType, long expiresInSeconds, VerifiedUser user) {}
    public record VerifiedUser(UUID id, String phone, List<String> platformRoles) {}
}
