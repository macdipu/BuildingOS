package com.buildingos.backoffice.onboarding.application;

import com.buildingos.backoffice.shared.application.BusinessRuleException;
import java.util.UUID;

public final class OnboardingErrors {
    private OnboardingErrors() {}

    /** Also returned for sessions the calling agent is not assigned to. */
    public static BusinessRuleException notFound(UUID id) {
        return BusinessRuleException.notFound("ONBOARDING_SESSION_NOT_FOUND",
                "Onboarding session " + id + " does not exist");
    }

    public static BusinessRuleException assigneeNotAgent(UUID userId) {
        return BusinessRuleException.unprocessable("ASSIGNEE_NOT_ONBOARDING_AGENT",
                "User " + userId + " does not hold the ONBOARDING_AGENT platform role");
    }

    public static BusinessRuleException unknownBuilding(UUID buildingId) {
        return BusinessRuleException.unprocessable("BUILDING_NOT_FOUND", "Building " + buildingId + " does not exist");
    }
}
