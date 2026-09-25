package com.buildingos.backoffice.supportsession.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Four-eyes request to use a high-risk scope in one support session (§149.12, D-10, D-36d). Immutable.
 * {@code decidedBy} is the SUPER_ADMIN who approved or denied (column {@code approved_by}).
 */
public record ElevatedApprovalRequest(UUID id, UUID supportSessionId, SupportScope requestedScope,
        ElevatedApprovalStatus status, UUID decidedBy, Instant requestedAt, Instant decidedAt, String decisionReason) {
    public ElevatedApprovalRequest {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(supportSessionId, "supportSessionId");
        Objects.requireNonNull(requestedScope, "requestedScope");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }

    /** A new PENDING request; only high-risk scopes need (and may have) one. */
    public static ElevatedApprovalRequest request(UUID sessionId, SupportScope scope, Instant now) {
        if (scope == null) {
            throw SupportSessionRuleException.invalid("scope is required");
        }
        if (!scope.isHighRisk()) {
            throw SupportSessionRuleException.invalid(scope + " is not a high-risk scope and needs no approval");
        }
        return new ElevatedApprovalRequest(UUID.randomUUID(), sessionId, scope, ElevatedApprovalStatus.PENDING, null,
                now, null, null);
    }

    public boolean isPending() { return status == ElevatedApprovalStatus.PENDING; }

    /** Optional decision reason. */
    public ElevatedApprovalRequest approve(UUID approver, String reason, Instant now) {
        requirePending();
        return decide(ElevatedApprovalStatus.APPROVED, approver, SupportSession.optionalReason(reason), now);
    }

    /** Decision reason required. */
    public ElevatedApprovalRequest deny(UUID approver, String reason, Instant now) {
        requirePending();
        String text = SupportSession.optionalReason(reason);
        if (text == null) {
            throw SupportSessionRuleException.invalid("reason is required to deny");
        }
        return decide(ElevatedApprovalStatus.DENIED, approver, text, now);
    }

    private void requirePending() {
        if (!isPending()) {
            throw SupportSessionRuleException.notPending(status);
        }
    }

    private ElevatedApprovalRequest decide(ElevatedApprovalStatus next, UUID approver, String reason, Instant now) {
        Objects.requireNonNull(approver, "approver");
        return new ElevatedApprovalRequest(id, supportSessionId, requestedScope, next, approver, requestedAt, now,
                reason);
    }
}
