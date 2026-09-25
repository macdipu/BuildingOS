package com.buildingos.backoffice.onboarding.domain.model;

/** D-10 fine-grained onboarding access scopes ({@code AssistedOnboardingSession.access_scope}). */
public enum OnboardingScope {
    ONBOARDING_VIEW_BUILDING,
    ONBOARDING_EDIT_BUILDING_INFO,
    ONBOARDING_MANAGE_FLOORS_UNITS,
    ONBOARDING_MANAGE_COMMITTEE_STAFF,
    ONBOARDING_CONFIGURE_MAINTENANCE,
    ONBOARDING_CONFIGURE_RENT_MANAGEMENT,
    ONBOARDING_CONFIGURE_PAYMENT_METHODS,
    ONBOARDING_SEND_INVITATIONS,
    ONBOARDING_ACTIVATE_BUILDING
}
