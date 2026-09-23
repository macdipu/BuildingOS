package com.buildingos.account.auth.presentation.rest;

import com.buildingos.account.auth.application.startotp.OtpRateLimitedException;
import com.buildingos.account.auth.application.startotp.StartOtpUseCase;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpResult;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpUseCase;
import com.buildingos.account.auth.presentation.rest.mapper.AuthApiMapper;
import com.buildingos.account.auth.presentation.rest.request.StartOtpRequest;
import com.buildingos.account.auth.presentation.rest.request.VerifyOtpRequest;
import com.buildingos.account.auth.presentation.rest.response.StartOtpResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.platform.web.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Auth transport; provider and signing configuration fail closed at startup. */
@RestController
@RequestMapping("/api/v1/auth/otp")
public class AuthController {
    private final StartOtpUseCase start;
    private final VerifyOtpUseCase verify;

    public AuthController(StartOtpUseCase start, VerifyOtpUseCase verify) {
        this.start = start;
        this.verify = verify;
    }

    @ExceptionHandler(OtpRateLimitedException.class)
    public ResponseEntity<?> rateLimited(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                ApiError.of("OTP_RATE_LIMITED",
                        "Please wait before requesting another code", CorrelationFilter.traceId(request)));
    }

    @PostMapping("/start")
    public ApiEnvelope<StartOtpResponse> start(@Valid @RequestBody StartOtpRequest request,
            HttpServletRequest httpRequest) {
        var result = start.execute(AuthApiMapper.toCommand(request));
        return ApiEnvelope.of(AuthApiMapper.toResponse(result), CorrelationFilter.traceId(httpRequest));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String traceId = CorrelationFilter.traceId(httpRequest);
        var result = verify.execute(AuthApiMapper.toCommand(request));
        if (result instanceof VerifyOtpResult.Verified verified) {
            return ResponseEntity.ok(ApiEnvelope.of(AuthApiMapper.toResponse(verified), traceId));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthApiMapper.toError((VerifyOtpResult.Rejected) result, traceId));
    }
}
