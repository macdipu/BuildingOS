package com.buildingos.backoffice.supportsession.application;

import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import java.util.List;

/** A support session with its elevated approval requests (oldest first). */
public record SupportSessionDetails(SupportSession session, List<ElevatedApprovalRequest> approvals) {
    public SupportSessionDetails {
        approvals = List.copyOf(approvals);
    }
}
