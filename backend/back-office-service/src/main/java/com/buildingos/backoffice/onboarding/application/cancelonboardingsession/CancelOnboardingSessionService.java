package com.buildingos.backoffice.onboarding.application.cancelonboardingsession;

import com.buildingos.backoffice.onboarding.application.OnboardingAccess;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionChanges;
import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

/** SUPER_ADMIN/PLATFORM_ADMIN only: to CANCELLED (D-33c). */
public final class CancelOnboardingSessionService implements CancelOnboardingSessionUseCase {
    private final OnboardingSessionChanges changes;

    public CancelOnboardingSessionService(OnboardingSessionChanges changes) {
        this.changes = changes;
    }

    @Override
    public AssistedOnboardingSession execute(Actor actor, OnboardingTransitionCommand command) {
        return changes.apply(actor, command, OnboardingAccess.Permission.OPERATOR,
                (current, now) -> current.cancel(now));
    }
}
