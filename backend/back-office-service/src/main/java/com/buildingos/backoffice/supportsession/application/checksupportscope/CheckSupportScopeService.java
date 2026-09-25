package com.buildingos.backoffice.supportsession.application.checksupportscope;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.SupportAccess;
import com.buildingos.backoffice.supportsession.application.SupportErrors;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionRuleException;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;

/** D-36: whether the session may use a scope now (ALLOWED | PENDING_APPROVAL | NOT_GRANTED | SESSION_ENDED). */
public final class CheckSupportScopeService implements CheckSupportScopeUseCase {
    private final SupportSessionRepository sessions;
    private final ElevatedApprovalRequestRepository approvals;
    private final SupportSessionExpiry expiry;

    public CheckSupportScopeService(SupportSessionRepository sessions, ElevatedApprovalRequestRepository approvals,
            SupportSessionExpiry expiry) {
        this.sessions = sessions;
        this.approvals = approvals;
        this.expiry = expiry;
    }

    @Override
    public SupportScopeCheck execute(Actor actor, CheckSupportScopeQuery query) {
        SupportAccess.requireSupportUser(actor);
        if (query.scope() == null) {
            throw SupportSessionRuleException.invalid("scope is required");
        }
        expiry.expireIfDue(query.sessionId());
        var session = sessions.findById(query.sessionId())
                .filter(found -> SupportAccess.canSee(actor, found))
                .orElseThrow(() -> SupportErrors.notFound(query.sessionId()));
        var result = session.check(query.scope(), approvals.findBySession(session.id()), expiry.now());
        return new SupportScopeCheck(session.id(), query.scope(), result);
    }
}
