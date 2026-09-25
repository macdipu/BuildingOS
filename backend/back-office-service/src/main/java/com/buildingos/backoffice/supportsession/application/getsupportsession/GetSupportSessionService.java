package com.buildingos.backoffice.supportsession.application.getsupportsession;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.SupportAccess;
import com.buildingos.backoffice.supportsession.application.SupportErrors;
import com.buildingos.backoffice.supportsession.application.SupportSessionDetails;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;

public final class GetSupportSessionService implements GetSupportSessionUseCase {
    private final SupportSessionRepository sessions;
    private final ElevatedApprovalRequestRepository approvals;
    private final SupportSessionExpiry expiry;

    public GetSupportSessionService(SupportSessionRepository sessions, ElevatedApprovalRequestRepository approvals,
            SupportSessionExpiry expiry) {
        this.sessions = sessions;
        this.approvals = approvals;
        this.expiry = expiry;
    }

    @Override
    public SupportSessionDetails execute(Actor actor, GetSupportSessionQuery query) {
        SupportAccess.requireSupportUser(actor);
        expiry.expireIfDue(query.sessionId());
        var session = sessions.findById(query.sessionId())
                .filter(found -> SupportAccess.canSee(actor, found))
                .orElseThrow(() -> SupportErrors.notFound(query.sessionId()));
        return new SupportSessionDetails(session, approvals.findBySession(session.id()));
    }
}
