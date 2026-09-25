package com.buildingos.backoffice.onboarding.application.awaitonboardingcustomer;

import com.buildingos.backoffice.onboarding.application.OnboardingAccess;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionChanges;
import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

/** Assigned agent only: IN_PROGRESS to WAITING_FOR_CUSTOMER (D-33c). */
public final class AwaitOnboardingCustomerService implements AwaitOnboardingCustomerUseCase {
    private final OnboardingSessionChanges changes;

    public AwaitOnboardingCustomerService(OnboardingSessionChanges changes) {
        this.changes = changes;
    }

    @Override
    public AssistedOnboardingSession execute(Actor actor, OnboardingTransitionCommand command) {
        return changes.apply(actor, command, OnboardingAccess.Permission.ASSIGNED_AGENT,
                (current, now) -> current.awaitCustomer(now));
    }
}
