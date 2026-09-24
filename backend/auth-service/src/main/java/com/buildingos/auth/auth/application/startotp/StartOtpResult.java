package com.buildingos.auth.auth.application.startotp;

import java.time.Instant;
import java.util.UUID;

public record StartOtpResult(UUID attemptId, Instant expiresAt) {}
