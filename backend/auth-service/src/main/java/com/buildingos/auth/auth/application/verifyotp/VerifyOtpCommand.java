package com.buildingos.auth.auth.application.verifyotp;

import java.util.UUID;

public record VerifyOtpCommand(UUID attemptId, String phone, String code) {}
