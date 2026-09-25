package com.buildingos.backoffice.onboarding.presentation.rest.response;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OnboardingSessionResponse(UUID id, UUID buildingId, UUID assignedAgentUserId, UUID requestedByUserId,
        String status, List<String> accessScope, String reason, Instant startedAt, Instant expiresAt,
        Instant completedAt, String notes) {
    public static OnboardingSessionResponse of(AssistedOnboardingSession s) {
        return new OnboardingSessionResponse(s.id(), s.buildingId(), s.assignedAgentUserId(), s.requestedByUserId(),
                s.status().name(), s.accessScope().stream().map(Enum::name).toList(), s.reason(), s.startedAt(),
                s.expiresAt(), s.completedAt(), s.notes());
    }
}
