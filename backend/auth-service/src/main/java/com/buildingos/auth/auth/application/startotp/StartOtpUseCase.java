package com.buildingos.auth.auth.application.startotp;

public interface StartOtpUseCase {
    StartOtpResult execute(StartOtpCommand command);
}
