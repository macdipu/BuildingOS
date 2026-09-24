package com.buildingos.auth.auth.application.verifyotp;

public interface VerifyOtpUseCase {
    VerifyOtpResult execute(VerifyOtpCommand command);
}
