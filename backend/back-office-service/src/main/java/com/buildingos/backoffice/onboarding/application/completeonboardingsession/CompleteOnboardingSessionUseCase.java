package com.buildingos.backoffice.onboarding.application.completeonboardingsession;

import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

public interface CompleteOnboardingSessionUseCase {
    AssistedOnboardingSession execute(Actor actor, OnboardingTransitionCommand command);
}
