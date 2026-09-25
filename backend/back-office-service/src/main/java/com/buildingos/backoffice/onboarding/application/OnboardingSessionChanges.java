package com.buildingos.backoffice.onboarding.application;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Instant;
import java.util.function.BiFunction;

/**
 * One audited status change on a locked session: caller role (403), lazy expiry, visibility (404), permission (403),
 * then the domain transition (409 when ended or not allowed from the current status).
 */
public final class OnboardingSessionChanges {
    private final AssistedOnboardingSessionRepository sessions;
    private final LifecycleTransitionRepository transitions;
    private final OnboardingSessionExpiry expiry;
    private final UnitOfWork unitOfWork;

    public OnboardingSessionChanges(AssistedOnboardingSessionRepository sessions,
            LifecycleTransitionRepository transitions, OnboardingSessionExpiry expiry, UnitOfWork unitOfWork) {
        this.sessions = sessions;
        this.transitions = transitions;
        this.expiry = expiry;
        this.unitOfWork = unitOfWork;
    }

    public AssistedOnboardingSession apply(Actor actor, OnboardingTransitionCommand command,
            OnboardingAccess.Permission permission,
            BiFunction<AssistedOnboardingSession, Instant, AssistedOnboardingSession> change) {
        OnboardingAccess.requireViewer(actor);
        String reason = AssistedOnboardingSession.transitionReason(command.reason());
        expiry.expireIfDue(command.sessionId());
        return unitOfWork.inTransaction(() -> {
            var current = sessions.findByIdForUpdate(command.sessionId())
                    .filter(session -> OnboardingAccess.canSee(actor, session))
                    .orElseThrow(() -> OnboardingErrors.notFound(command.sessionId()));
            if (!OnboardingAccess.permits(permission, actor, current)) {
                throw new NotPermittedException();
            }
            Instant now = expiry.now();
            var next = change.apply(current, now);
            sessions.update(next);
            transitions.append(LifecycleTransition.of(EntityType.ASSISTED_ONBOARDING_SESSION, current.id(),
                    current.status(), next.status(), actor.userId(), reason, now));
            return next;
        });
    }
}
