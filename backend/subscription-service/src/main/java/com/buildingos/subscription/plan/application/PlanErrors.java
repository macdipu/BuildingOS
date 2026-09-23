package com.buildingos.subscription.plan.application;

import com.buildingos.subscription.shared.application.BusinessRuleException;
import java.util.UUID;

public final class PlanErrors {
    private PlanErrors() {}

    public static BusinessRuleException notFound(UUID id) {
        return BusinessRuleException.notFound("PLAN_NOT_FOUND", "Plan " + id + " does not exist");
    }

    public static BusinessRuleException retired() {
        return BusinessRuleException.conflict("PLAN_RETIRED", "Plan is retired");
    }
}
