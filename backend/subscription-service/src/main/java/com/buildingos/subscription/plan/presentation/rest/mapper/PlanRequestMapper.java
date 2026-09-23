package com.buildingos.subscription.plan.presentation.rest.mapper;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.plan.application.createplan.CreatePlanCommand;
import com.buildingos.subscription.plan.application.updateplan.UpdatePlanCommand;
import com.buildingos.subscription.plan.domain.model.PlanCode;
import com.buildingos.subscription.plan.presentation.rest.request.PlanRequest;
import com.buildingos.subscription.shared.presentation.rest.Enums;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PlanRequestMapper {
    private PlanRequestMapper() {}

    public static CreatePlanCommand toCreate(PlanRequest request) {
        return new CreatePlanCommand(new PlanCode(request.code()), request.name(), cycles(request),
                selfService(request), Entitlements.fromKeys(request.entitlements()));
    }

    public static UpdatePlanCommand toUpdate(UUID planId, PlanRequest request) {
        return new UpdatePlanCommand(planId, request.name(), cycles(request), selfService(request),
                Entitlements.fromKeys(request.entitlements()));
    }

    private static Set<BillingCycle> cycles(PlanRequest request) {
        if (request.billingCycles() == null) {
            throw new IllegalArgumentException("billingCycles is required");
        }
        return request.billingCycles().stream().map(value -> Enums.parse(BillingCycle.class, value, "billingCycles"))
                .collect(Collectors.toSet());
    }

    private static boolean selfService(PlanRequest request) {
        if (request.selfService() == null) {
            throw new IllegalArgumentException("selfService is required");
        }
        return request.selfService();
    }
}
