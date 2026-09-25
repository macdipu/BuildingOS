package com.buildingos.backoffice.onboarding.application.listonboardingsessions;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;

public interface ListOnboardingSessionsUseCase {
    Page<AssistedOnboardingSession> execute(Actor actor, ListOnboardingSessionsQuery query);
}
