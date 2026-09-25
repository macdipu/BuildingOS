package com.buildingos.backoffice.supportsession.application.requestelevatedapproval;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import com.buildingos.backoffice.supportsession.application.SupportAccess;
import com.buildingos.backoffice.supportsession.application.SupportErrors;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalStatus;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionRuleException;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;

/**
 * D-36d: the session owner requests a high-risk scope on their active session with a reason. One PENDING request per
 * session and scope; a denied scope may be requested again; an already approved scope is a 409.
 */
public final class RequestElevatedApprovalService implements RequestElevatedApprovalUseCase {
    private final SupportSessionRepository sessions;
    private final ElevatedApprovalRequestRepository approvals;
    private final LifecycleTransitionRepository transitions;
    private final SupportSessionExpiry expiry;
    private final UnitOfWork unitOfWork;

    public RequestElevatedApprovalService(SupportSessionRepository sessions,
            ElevatedApprovalRequestRepository approvals, LifecycleTransitionRepository transitions,
            SupportSessionExpiry expiry, UnitOfWork unitOfWork) {
        this.sessions = sessions;
        this.approvals = approvals;
        this.transitions = transitions;
        this.expiry = expiry;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public ElevatedApprovalRequest execute(Actor actor, RequestElevatedApprovalCommand command) {
        SupportAccess.requireSupportUser(actor);
        String reason = SupportSession.optionalReason(command.reason());
        if (reason == null) {
            throw SupportSessionRuleException.invalid("reason is required");
        }
        expiry.expireIfDue(command.sessionId());
        return unitOfWork.inTransaction(() -> {
            var session = sessions.findByIdForUpdate(command.sessionId())
                    .filter(found -> SupportAccess.canSee(actor, found))
                    .orElseThrow(() -> SupportErrors.notFound(command.sessionId()));
            if (!SupportAccess.isOwner(actor, session)) {
                throw new NotPermittedException();
            }
            var now = expiry.now();
            var request = ElevatedApprovalRequest.request(session.id(), command.scope(), now);
            session.requireActive(now);
            for (var existing : approvals.findBySession(session.id())) {
                if (existing.requestedScope() != request.requestedScope()) {
                    continue;
                }
                if (existing.isPending()) {
                    throw SupportSessionRuleException.alreadyPending(request.requestedScope());
                }
                if (existing.status() == ElevatedApprovalStatus.APPROVED) {
                    throw SupportSessionRuleException.alreadyApproved(request.requestedScope());
                }
            }
            var next = session.withRequestedScope(request.requestedScope());
            if (next != session) {
                sessions.update(next);
            }
            approvals.insert(request);
            transitions.append(LifecycleTransition.of(EntityType.ELEVATED_APPROVAL_REQUEST, request.id(), null,
                    request.status(), actor.userId(), reason, now));
            return request;
        });
    }
}
