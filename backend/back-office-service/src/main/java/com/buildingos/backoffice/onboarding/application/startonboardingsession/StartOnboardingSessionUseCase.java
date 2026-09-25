package com.buildingos.backoffice.onboarding.application.startonboardingsession;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

public interface StartOnboardingSessionUseCase {
    AssistedOnboardingSession execute(Actor actor, StartOnboardingSessionCommand command);
}
