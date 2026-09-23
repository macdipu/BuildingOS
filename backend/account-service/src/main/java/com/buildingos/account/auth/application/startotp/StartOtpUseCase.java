package com.buildingos.account.auth.application.startotp;

public interface StartOtpUseCase {
    StartOtpResult execute(StartOtpCommand command);
}
