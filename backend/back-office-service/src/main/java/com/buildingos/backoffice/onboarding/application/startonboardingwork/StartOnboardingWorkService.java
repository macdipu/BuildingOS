package com.buildingos.backoffice.onboarding.application.startonboardingwork;

import com.buildingos.backoffice.onboarding.application.OnboardingAccess;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionChanges;
import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

/** Assigned agent only: ASSIGNED | WAITING_FOR_CUSTOMER to IN_PROGRESS (D-33c). */
public final class StartOnboardingWorkService implements StartOnboardingWorkUseCase {
    private final OnboardingSessionChanges changes;

    public StartOnboardingWorkService(OnboardingSessionChanges changes) {
        this.changes = changes;
    }

    @Override
    public AssistedOnboardingSession execute(Actor actor, OnboardingTransitionCommand command) {
        return changes.apply(actor, command, OnboardingAccess.Permission.ASSIGNED_AGENT,
                (current, now) -> current.startWork(now));
    }
}
