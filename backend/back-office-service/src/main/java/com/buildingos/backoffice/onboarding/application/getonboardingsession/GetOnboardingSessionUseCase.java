package com.buildingos.backoffice.onboarding.application.getonboardingsession;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;

public interface GetOnboardingSessionUseCase {
    AssistedOnboardingSession execute(Actor actor, GetOnboardingSessionQuery query);
}
