package com.buildingos.subscription.subscription.presentation.rest.request;

import java.util.UUID;

public record SubscribeRequest(UUID planId, String billingCycle) {
    public UUID requiredPlanId() {
        if (planId == null) {
            throw new IllegalArgumentException("planId is required");
        }
        return planId;
    }
}
