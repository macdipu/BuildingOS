package com.buildingos.backoffice.supportsession.application.denyelevatedapproval;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.DecideElevatedApprovalCommand;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;

public interface DenyElevatedApprovalUseCase {
    ElevatedApprovalRequest execute(Actor actor, DecideElevatedApprovalCommand input);
}
