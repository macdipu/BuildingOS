package com.buildingos.backoffice.onboarding.presentation.rest.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartOnboardingSessionRequest(UUID buildingId, UUID assignedAgentUserId, List<String> accessScope,
        String reason, String notes, Instant expiresAt) {}
