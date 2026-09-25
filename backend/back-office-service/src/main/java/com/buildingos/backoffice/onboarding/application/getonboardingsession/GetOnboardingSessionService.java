package com.buildingos.backoffice.onboarding.application.getonboardingsession;

import com.buildingos.backoffice.onboarding.application.OnboardingAccess;
import com.buildingos.backoffice.onboarding.application.OnboardingErrors;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionExpiry;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
import com.buildingos.backoffice.shared.application.Actor;

public final class GetOnboardingSessionService implements GetOnboardingSessionUseCase {
    private final AssistedOnboardingSessionRepository sessions;
    private final OnboardingSessionExpiry expiry;

    public GetOnboardingSessionService(AssistedOnboardingSessionRepository sessions, OnboardingSessionExpiry expiry) {
        this.sessions = sessions;
        this.expiry = expiry;
    }

    @Override
    public AssistedOnboardingSession execute(Actor actor, GetOnboardingSessionQuery query) {
        OnboardingAccess.requireViewer(actor);
        expiry.expireIfDue(query.sessionId());
        return sessions.findById(query.sessionId())
                .filter(session -> OnboardingAccess.canSee(actor, session))
                .orElseThrow(() -> OnboardingErrors.notFound(query.sessionId()));
    }
}
