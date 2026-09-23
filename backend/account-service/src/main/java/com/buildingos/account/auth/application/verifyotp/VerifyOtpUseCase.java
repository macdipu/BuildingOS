package com.buildingos.account.auth.application.verifyotp;

public interface VerifyOtpUseCase {
    VerifyOtpResult execute(VerifyOtpCommand command);
}
