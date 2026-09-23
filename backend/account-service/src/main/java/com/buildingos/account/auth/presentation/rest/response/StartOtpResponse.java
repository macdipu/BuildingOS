package com.buildingos.account.auth.presentation.rest.response;

import java.util.UUID;

public record StartOtpResponse(UUID attemptId, String expiresAt) {}
