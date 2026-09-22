package com.buildingos.identity.auth.application.port.out;

/**
 * Verifies a submitted OTP code. Replaceable adapter boundary: the development
 * implementation checks a fixed code; a future real provider delegates to its vendor.
 */
public interface OtpCodeVerifier {
    boolean isValidCode(String phone, String code);
}
