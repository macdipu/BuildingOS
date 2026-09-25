package com.buildingos.backoffice.onboarding.application.listonboardingsessions;

import com.buildingos.backoffice.onboarding.application.OnboardingAccess;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionExpiry;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionFilter;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;
import java.util.List;

/** Operators list every session; an agent lists only their own (another agent id filter yields nothing). */
public final class ListOnboardingSessionsService implements ListOnboardingSessionsUseCase {
    private final AssistedOnboardingSessionRepository sessions;
    private final OnboardingSessionExpiry expiry;

    public ListOnboardingSessionsService(AssistedOnboardingSessionRepository sessions, OnboardingSessionExpiry expiry) {
        this.sessions = sessions;
        this.expiry = expiry;
    }

    @Override
    public Page<AssistedOnboardingSession> execute(Actor actor, ListOnboardingSessionsQuery query) {
        OnboardingAccess.requireViewer(actor);
        Page.validate(query.page(), query.size());
        var agentFilter = query.agentUserId();
        if (!actor.isOperator()) {
            if (agentFilter != null && !agentFilter.equals(actor.userId())) {
                return new Page<>(List.of(), query.page(), query.size(), 0);
            }
            agentFilter = actor.userId();
        }
        expiry.expireAllDue();
        var filter = new OnboardingSessionFilter(query.status(), query.buildingId(), agentFilter);
        var items = sessions.search(filter, query.page() * query.size(), query.size());
        return new Page<>(items, query.page(), query.size(), sessions.count(filter));
    }
}
