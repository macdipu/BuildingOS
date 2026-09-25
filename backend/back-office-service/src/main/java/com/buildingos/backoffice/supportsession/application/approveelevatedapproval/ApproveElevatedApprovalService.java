package com.buildingos.backoffice.supportsession.application.approveelevatedapproval;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.DecideElevatedApprovalCommand;
import com.buildingos.backoffice.supportsession.application.ElevatedApprovalDecisions;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;

/** D-36d: a SUPER_ADMIN other than the session owner approves a PENDING request (reason optional). */
public final class ApproveElevatedApprovalService implements ApproveElevatedApprovalUseCase {
    private final ElevatedApprovalDecisions decisions;

    public ApproveElevatedApprovalService(ElevatedApprovalDecisions decisions) {
        this.decisions = decisions;
    }

    @Override
    public ElevatedApprovalRequest execute(Actor actor, DecideElevatedApprovalCommand command) {
        return decisions.apply(actor, command,
                (current, now) -> current.approve(actor.userId(), command.reason(), now));
    }
}
