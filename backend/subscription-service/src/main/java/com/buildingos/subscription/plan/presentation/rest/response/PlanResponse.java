package com.buildingos.subscription.plan.presentation.rest.response;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlanResponse(UUID id, String code, String name, String status, List<String> billingCycles,
        boolean selfService, Map<String, Object> entitlements, Instant createdAt, Instant updatedAt) {
    public static PlanResponse of(SubscriptionPlan plan) {
        return new PlanResponse(plan.id(), plan.code().value(), plan.name(), plan.status().name(),
                plan.billingCycles().stream().map(Enum::name).sorted().toList(), plan.selfService(),
                plan.entitlements().toKeys(), plan.createdAt(), plan.updatedAt());
    }
}
