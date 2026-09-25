package com.buildingos.backoffice.supportsession.presentation.rest.response;

import com.buildingos.backoffice.supportsession.application.SupportSessionDetails;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SupportSessionDetailResponse(UUID id, UUID platformUserId, UUID targetUserId, UUID buildingId,
        String status, String reason, List<String> permissionScope, Instant startedAt, Instant expiresAt,
        Instant endedAt, List<ElevatedApprovalResponse> elevatedApprovals) {
    public static SupportSessionDetailResponse of(SupportSessionDetails details) {
        var s = SupportSessionResponse.of(details.session());
        return new SupportSessionDetailResponse(s.id(), s.platformUserId(), s.targetUserId(), s.buildingId(),
                s.status(), s.reason(), s.permissionScope(), s.startedAt(), s.expiresAt(), s.endedAt(),
                details.approvals().stream().map(ElevatedApprovalResponse::of).toList());
    }
}
