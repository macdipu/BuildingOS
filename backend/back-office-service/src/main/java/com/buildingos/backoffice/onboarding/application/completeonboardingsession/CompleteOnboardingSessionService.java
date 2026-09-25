package com.buildingos.backoffice.onboarding.application.completeonboardingsession;

import com.buildingos.backoffice.onboarding.application.OnboardingAccess;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionChanges;
import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

/** Assigned agent or SUPER_ADMIN/PLATFORM_ADMIN: to COMPLETED (D-33c). */
public final class CompleteOnboardingSessionService implements CompleteOnboardingSessionUseCase {
    private final OnboardingSessionChanges changes;

    public CompleteOnboardingSessionService(OnboardingSessionChanges changes) {
        this.changes = changes;
    }

    @Override
    public AssistedOnboardingSession execute(Actor actor, OnboardingTransitionCommand command) {
        return changes.apply(actor, command, OnboardingAccess.Permission.ASSIGNED_AGENT_OR_OPERATOR,
                (current, now) -> current.complete(now));
    }
}
