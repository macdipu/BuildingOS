package com.buildingos.subscription.plan.application.createplan;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.plan.domain.model.PlanCode;
import java.util.Set;

public record CreatePlanCommand(PlanCode code, String name, Set<BillingCycle> billingCycles,
        boolean selfService, Entitlements entitlements) {}
