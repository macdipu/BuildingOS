package com.buildingos.backoffice.supportsession.application;

import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionStatus;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Lazy expiry (D-36b): a session not ended by {@code expires_at} is persisted as ended at {@code expires_at} with a
 * system transition (no actor), committed in its own transaction before any read or change looks at it. Its pending
 * approvals stay PENDING but can no longer be decided or used.
 */
public final class SupportSessionExpiry {
    static final int BATCH = 100;

    private final SupportSessionRepository sessions;
    private final LifecycleTransitionRepository transitions;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public SupportSessionExpiry(SupportSessionRepository sessions, LifecycleTransitionRepository transitions,
            UnitOfWork unitOfWork, Clock clock) {
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

    private void expire(SupportSession session, Instant now) {
        session.expire(now).ifPresent(next -> {
            sessions.update(next);
            transitions.append(LifecycleTransition.of(EntityType.SUPPORT_SESSION, session.id(),
                    SupportSessionStatus.ACTIVE, next.status(), null, null, now));
        });
    }
}
