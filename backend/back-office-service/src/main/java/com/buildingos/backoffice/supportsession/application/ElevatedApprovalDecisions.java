package com.buildingos.backoffice.supportsession.application;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalStatus;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;
import java.time.Instant;
import java.util.function.BiFunction;

/**
 * One audited decision (D-36d): SUPER_ADMIN only (403), lazy expiry of the session, approval exists (404), approver
 * is not the session owner (403), session still active (409), request still PENDING (409).
 */
public final class ElevatedApprovalDecisions {
    private final SupportSessionRepository sessions;
    private final ElevatedApprovalRequestRepository approvals;
    private final LifecycleTransitionRepository transitions;
    private final SupportSessionExpiry expiry;
    private final UnitOfWork unitOfWork;

    public ElevatedApprovalDecisions(SupportSessionRepository sessions, ElevatedApprovalRequestRepository approvals,
            LifecycleTransitionRepository transitions, SupportSessionExpiry expiry, UnitOfWork unitOfWork) {
        this.sessions = sessions;
        this.approvals = approvals;
        this.transitions = transitions;
        this.expiry = expiry;
        this.unitOfWork = unitOfWork;
    }

    public ElevatedApprovalRequest apply(Actor actor, DecideElevatedApprovalCommand command,
            BiFunction<ElevatedApprovalRequest, Instant, ElevatedApprovalRequest> decision) {
        if (!actor.isSuperAdmin()) {
            throw new NotPermittedException();
        }
        var found = approvals.findById(command.approvalId())
                .orElseThrow(() -> SupportErrors.approvalNotFound(command.approvalId()));
        expiry.expireIfDue(found.supportSessionId());
        return unitOfWork.inTransaction(() -> {
            var session = sessions.findByIdForUpdate(found.supportSessionId())
                    .orElseThrow(() -> SupportErrors.approvalNotFound(command.approvalId()));
            var current = approvals.findByIdForUpdate(command.approvalId())
                    .orElseThrow(() -> SupportErrors.approvalNotFound(command.approvalId()));
            if (!SupportAccess.mayDecide(actor, session)) {
                throw new NotPermittedException();
            }
            Instant now = expiry.now();
            session.requireActive(now);
            var next = decision.apply(current, now);
            approvals.update(next);
            transitions.append(LifecycleTransition.of(EntityType.ELEVATED_APPROVAL_REQUEST, current.id(),
                    ElevatedApprovalStatus.PENDING, next.status(), actor.userId(), next.decisionReason(), now));
            return next;
        });
    }
}
