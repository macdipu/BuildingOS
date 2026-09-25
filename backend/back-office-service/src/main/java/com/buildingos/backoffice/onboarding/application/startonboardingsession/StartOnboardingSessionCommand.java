package com.buildingos.backoffice.onboarding.application.startonboardingsession;

import com.buildingos.backoffice.onboarding.domain.model.OnboardingScope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartOnboardingSessionCommand(UUID buildingId, UUID assignedAgentUserId, List<OnboardingScope> accessScope,
        String reason, String notes, Instant expiresAt) {}
