package com.buildingos.backoffice.onboarding.application;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Lazy expiry (BOC-06, D-33d): an active session past {@code expires_at} is persisted as {@code EXPIRED} with a
 * system transition (no actor), committed in its own transaction before any read or change looks at it.
 */
public final class OnboardingSessionExpiry {
    static final int BATCH = 100;

    private final AssistedOnboardingSessionRepository sessions;
    private final LifecycleTransitionRepository transitions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public OnboardingSessionExpiry(AssistedOnboardingSessionRepository sessions,
            LifecycleTransitionRepository transitions, UnitOfWork unitOfWork, Clock clock) {
        this.sessions = sessions;
        this.transitions = transitions;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    /** Current time at database (microsecond) precision. */
    public Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    public void expireIfDue(UUID sessionId) {
        unitOfWork.inTransaction(() -> {
            Instant now = now();
            sessions.findByIdForUpdate(sessionId).ifPresent(session -> expire(session, now));
            return null;
        });
    }

    /** Every due session, in batches; rows locked by another transaction are skipped (that one expires them). */
    public void expireAllDue() {
        int expired;
        do {
            expired = unitOfWork.inTransaction(() -> {
                Instant now = now();
                var due = sessions.lockDueForExpiry(now, BATCH);
                due.forEach(session -> expire(session, now));
                return due.size();
            });
        } while (expired == BATCH);
    }

    private void expire(AssistedOnboardingSession session, Instant now) {
        session.expire(now).ifPresent(next -> {
            sessions.update(next);
            transitions.append(LifecycleTransition.of(EntityType.ASSISTED_ONBOARDING_SESSION, session.id(),
                    session.status(), next.status(), null, null, now));
        });
    }
}
