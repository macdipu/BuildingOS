package com.buildingos.backoffice.supportsession.application;

import com.buildingos.backoffice.shared.application.BusinessRuleException;
import java.util.UUID;

public final class SupportErrors {
    private SupportErrors() {}

    /** Also returned for another agent's sessions. */
    public static BusinessRuleException notFound(UUID id) {
        return BusinessRuleException.notFound("SUPPORT_SESSION_NOT_FOUND", "Support session " + id + " does not exist");
    }

    public static BusinessRuleException approvalNotFound(UUID id) {
        return BusinessRuleException.notFound("ELEVATED_APPROVAL_NOT_FOUND",
                "Elevated approval request " + id + " does not exist");
    }

    public static BusinessRuleException unknownTargetUser(UUID userId) {
        return BusinessRuleException.unprocessable("TARGET_USER_NOT_FOUND", "User " + userId + " does not exist");
    }

    public static BusinessRuleException unknownBuilding(UUID buildingId) {
        return BusinessRuleException.unprocessable("BUILDING_NOT_FOUND", "Building " + buildingId + " does not exist");
    }
}
