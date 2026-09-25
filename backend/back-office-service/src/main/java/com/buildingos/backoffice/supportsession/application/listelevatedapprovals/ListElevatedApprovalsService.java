package com.buildingos.backoffice.supportsession.application.listelevatedapprovals;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;
import com.buildingos.backoffice.supportsession.application.SupportAccess;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalFilter;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;

/** Admins list every request; a support agent only requests on their own sessions (D-36c visibility). */
public final class ListElevatedApprovalsService implements ListElevatedApprovalsUseCase {
    private final ElevatedApprovalRequestRepository approvals;
    private final SupportSessionExpiry expiry;

    public ListElevatedApprovalsService(ElevatedApprovalRequestRepository approvals, SupportSessionExpiry expiry) {
        this.approvals = approvals;
        this.expiry = expiry;
    }

    @Override
    public Page<ElevatedApprovalRequest> execute(Actor actor, ListElevatedApprovalsQuery query) {
        SupportAccess.requireSupportUser(actor);
        Page.validate(query.page(), query.size());
        expiry.expireAllDue();
        var filter = new ElevatedApprovalFilter(query.status(), actor.isOperator() ? null : actor.userId());
        var items = approvals.search(filter, query.page() * query.size(), query.size());
        return new Page<>(items, query.page(), query.size(), approvals.count(filter));
    }
}
