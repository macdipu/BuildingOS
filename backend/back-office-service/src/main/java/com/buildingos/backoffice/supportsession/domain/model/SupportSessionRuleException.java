package com.buildingos.backoffice.supportsession.domain.model;

import com.buildingos.backoffice.shared.domain.model.DomainRuleException;

/** A support-session or elevated-approval rule the request broke. */
public final class SupportSessionRuleException extends DomainRuleException {
    private SupportSessionRuleException(String code, Kind kind, String message) {
        super(code, kind, message);
    }

    public static SupportSessionRuleException invalid(String message) {
        return new SupportSessionRuleException("INVALID_REQUEST", Kind.INVALID, message);
    }

    /** The session was ended or has reached expires_at: nothing may change it or use its approvals. */
    public static SupportSessionRuleException ended() {
        return new SupportSessionRuleException("SESSION_ENDED", Kind.CONFLICT, "The support session has ended");
    }

    public static SupportSessionRuleException notPending(ElevatedApprovalStatus status) {
        return new SupportSessionRuleException("APPROVAL_NOT_PENDING", Kind.CONFLICT,
                "The elevated approval request is already " + status);
    }

    public static SupportSessionRuleException alreadyPending(SupportScope scope) {
        return new SupportSessionRuleException("APPROVAL_ALREADY_PENDING", Kind.CONFLICT,
                "An elevated approval for " + scope + " is already pending on this session");
    }

    public static SupportSessionRuleException alreadyApproved(SupportScope scope) {
        return new SupportSessionRuleException("APPROVAL_ALREADY_GRANTED", Kind.CONFLICT,
                scope + " is already approved on this session");
    }
}
