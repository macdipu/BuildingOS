package com.buildingos.backoffice.onboarding.application.awaitonboardingcustomer;

import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

public interface AwaitOnboardingCustomerUseCase {
    AssistedOnboardingSession execute(Actor actor, OnboardingTransitionCommand command);
}
