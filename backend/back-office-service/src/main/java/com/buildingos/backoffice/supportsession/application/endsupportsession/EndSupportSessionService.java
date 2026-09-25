package com.buildingos.backoffice.supportsession.application.endsupportsession;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import com.buildingos.backoffice.supportsession.application.SupportAccess;
import com.buildingos.backoffice.supportsession.application.SupportErrors;
import com.buildingos.backoffice.supportsession.application.SupportSessionDetails;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;

/** D-36c: the owner, SUPER_ADMIN or PLATFORM_ADMIN ends an active session; an ended or expired one is a 409. */
public final class EndSupportSessionService implements EndSupportSessionUseCase {
    private final SupportSessionRepository sessions;
    private final ElevatedApprovalRequestRepository approvals;
    private final LifecycleTransitionRepository transitions;
    private final SupportSessionExpiry expiry;
    private final UnitOfWork unitOfWork;

    public EndSupportSessionService(SupportSessionRepository sessions, ElevatedApprovalRequestRepository approvals,
            LifecycleTransitionRepository transitions, SupportSessionExpiry expiry, UnitOfWork unitOfWork) {
        this.sessions = sessions;
        this.approvals = approvals;
        this.transitions = transitions;
        this.expiry = expiry;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public SupportSessionDetails execute(Actor actor, EndSupportSessionCommand command) {
        SupportAccess.requireSupportUser(actor);
        String reason = SupportSession.optionalReason(command.reason());
        expiry.expireIfDue(command.sessionId());
        return unitOfWork.inTransaction(() -> {
            var current = sessions.findByIdForUpdate(command.sessionId())
                    .filter(session -> SupportAccess.canSee(actor, session))
                    .orElseThrow(() -> SupportErrors.notFound(command.sessionId()));
            if (!SupportAccess.mayEnd(actor, current)) {
                throw new NotPermittedException();
            }
            var now = expiry.now();
            var next = current.end(now);
            sessions.update(next);
            transitions.append(LifecycleTransition.of(EntityType.SUPPORT_SESSION, current.id(), current.status(),
                    next.status(), actor.userId(), reason, now));
            return new SupportSessionDetails(next, approvals.findBySession(next.id()));
        });
    }
}
