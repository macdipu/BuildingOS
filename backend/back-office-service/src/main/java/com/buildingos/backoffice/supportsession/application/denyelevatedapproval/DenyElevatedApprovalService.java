package com.buildingos.backoffice.supportsession.application.denyelevatedapproval;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.DecideElevatedApprovalCommand;
import com.buildingos.backoffice.supportsession.application.ElevatedApprovalDecisions;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;

/** D-36d: a SUPER_ADMIN other than the session owner denies a PENDING request (reason required). */
public final class DenyElevatedApprovalService implements DenyElevatedApprovalUseCase {
    private final ElevatedApprovalDecisions decisions;

    public DenyElevatedApprovalService(ElevatedApprovalDecisions decisions) {
        this.decisions = decisions;
    }

    @Override
    public ElevatedApprovalRequest execute(Actor actor, DecideElevatedApprovalCommand command) {
        return decisions.apply(actor, command,
                (current, now) -> current.deny(actor.userId(), command.reason(), now));
    }
}
