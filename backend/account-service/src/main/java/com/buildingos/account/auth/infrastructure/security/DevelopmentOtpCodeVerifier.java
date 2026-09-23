package com.buildingos.account.auth.infrastructure.security;

import com.buildingos.account.auth.application.port.out.OtpCodeVerifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local/test-only fixed-code OTP verifier (BOS-002 D-01, carried into BOS-010): accepts
 * exactly "000000". Sends no SMS, needs no vendor account. The {@code @Profile} guard is
 * the enforcement mechanism: outside local/test no {@link OtpCodeVerifier} bean exists,
 * so the application fails to start rather than silently falling back to a fixed code.
 */
@Component
@Profile({"local", "test"})
public final class DevelopmentOtpCodeVerifier implements OtpCodeVerifier {
    static final String FIXED_CODE = "000000";

    @Override
    public boolean isValidCode(String phone, String code) {
        return FIXED_CODE.equals(code);
    }
}
