package com.buildingos.backoffice.onboarding.application;

import java.util.UUID;

/** A status change on one session with an optional audit reason. */
public record OnboardingTransitionCommand(UUID sessionId, String reason) {}
