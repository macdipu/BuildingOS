package com.buildingos.backoffice.supportsession.application.approveelevatedapproval;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.DecideElevatedApprovalCommand;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;

public interface ApproveElevatedApprovalUseCase {
    ElevatedApprovalRequest execute(Actor actor, DecideElevatedApprovalCommand input);
}
