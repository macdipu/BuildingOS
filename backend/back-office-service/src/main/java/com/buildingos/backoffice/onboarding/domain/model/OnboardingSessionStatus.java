package com.buildingos.backoffice.onboarding.domain.model;

import java.util.EnumSet;
import java.util.Set;

/** BRD §149.11 statuses. {@code REQUESTED} (customer-initiated) is not created in F6. */
public enum OnboardingSessionStatus {
    REQUESTED, ASSIGNED, IN_PROGRESS, WAITING_FOR_CUSTOMER, COMPLETED, CANCELLED, EXPIRED;

    /** Statuses in which the session still grants access and can expire (BOC-06). */
    public static final Set<OnboardingSessionStatus> ACTIVE =
            EnumSet.of(REQUESTED, ASSIGNED, IN_PROGRESS, WAITING_FOR_CUSTOMER);

    public boolean isActive() { return ACTIVE.contains(this); }
}
