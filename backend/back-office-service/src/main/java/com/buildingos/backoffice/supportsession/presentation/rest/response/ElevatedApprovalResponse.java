package com.buildingos.backoffice.supportsession.presentation.rest.response;

import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import java.time.Instant;
import java.util.UUID;

public record ElevatedApprovalResponse(UUID id, UUID supportSessionId, String requestedScope, String status,
        UUID decidedBy, Instant requestedAt, Instant decidedAt, String decisionReason) {
    public static ElevatedApprovalResponse of(ElevatedApprovalRequest r) {
        return new ElevatedApprovalResponse(r.id(), r.supportSessionId(), r.requestedScope().name(),
                r.status().name(), r.decidedBy(), r.requestedAt(), r.decidedAt(), r.decisionReason());
    }
}
