package com.buildingos.backoffice.onboarding.application.listonboardingsessions;

import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus;
import java.util.UUID;

/** Optional filters; a null filter matches everything. */
public record ListOnboardingSessionsQuery(OnboardingSessionStatus status, UUID buildingId, UUID agentUserId, int page,
        int size) {}
