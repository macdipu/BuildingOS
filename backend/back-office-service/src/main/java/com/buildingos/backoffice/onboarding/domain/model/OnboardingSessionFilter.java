package com.buildingos.backoffice.onboarding.domain.model;

import java.util.UUID;

/** Optional list filters; a null field does not filter. */
public record OnboardingSessionFilter(OnboardingSessionStatus status, UUID buildingId, UUID agentUserId) {}
