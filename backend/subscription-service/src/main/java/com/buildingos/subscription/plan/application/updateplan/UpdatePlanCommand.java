package com.buildingos.subscription.plan.application.updateplan;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.catalog.domain.model.Entitlements;
import java.util.Set;
import java.util.UUID;

public record UpdatePlanCommand(UUID planId, String name, Set<BillingCycle> billingCycles,
        boolean selfService, Entitlements entitlements) {}
