package com.buildingos.backoffice.onboarding.domain.model;

import com.buildingos.backoffice.shared.domain.model.DomainRuleException;

/** An assisted-onboarding rule the request broke. */
public final class OnboardingSessionRuleException extends DomainRuleException {
    private OnboardingSessionRuleException(String code, Kind kind, String message) {
        super(code, kind, message);
    }

    public static OnboardingSessionRuleException invalid(String message) {
        return new OnboardingSessionRuleException("INVALID_REQUEST", Kind.INVALID, message);
    }

    /** The session is COMPLETED, CANCELLED or EXPIRED (or past expires_at): nothing may change it. */
    public static OnboardingSessionRuleException ended(OnboardingSessionStatus status) {
        return new OnboardingSessionRuleException("SESSION_ENDED", Kind.CONFLICT,
                "The onboarding session has ended (" + status + ")");
    }

    public static OnboardingSessionRuleException invalidTransition(OnboardingSessionStatus from, String action) {
        return new OnboardingSessionRuleException("INVALID_TRANSITION", Kind.CONFLICT,
                "Cannot " + action + " an onboarding session in status " + from);
    }
}
