package com.buildingos.backoffice.supportsession.application.listelevatedapprovals;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;

public interface ListElevatedApprovalsUseCase {
    Page<ElevatedApprovalRequest> execute(Actor actor, ListElevatedApprovalsQuery input);
}
