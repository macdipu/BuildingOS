package com.buildingos.account.auth.application.startotp;

public final class OtpRateLimitedException extends RuntimeException {
    public OtpRateLimitedException() { super("OTP start limit reached"); }
}
