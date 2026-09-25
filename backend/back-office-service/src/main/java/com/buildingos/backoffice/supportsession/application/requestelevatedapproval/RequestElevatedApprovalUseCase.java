package com.buildingos.backoffice.supportsession.application.requestelevatedapproval;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;

public interface RequestElevatedApprovalUseCase {
    ElevatedApprovalRequest execute(Actor actor, RequestElevatedApprovalCommand input);
}
