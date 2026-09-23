package com.buildingos.subscription.plan.domain.model;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.catalog.domain.model.Entitlements;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/** Operator-defined plan (D-08); no plan is seeded or hardcoded. */
public record SubscriptionPlan(UUID id, PlanCode code, String name, PlanStatus status,
        Set<BillingCycle> billingCycles, boolean selfService, Entitlements entitlements,
        Instant createdAt, Instant updatedAt) {
    private static final int MAX_NAME_LENGTH = 200;

    public SubscriptionPlan {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Plan name must be 1-" + MAX_NAME_LENGTH + " characters");
        }
        if (billingCycles == null || billingCycles.isEmpty()) {
            throw new IllegalArgumentException("A plan must offer at least one billing cycle");
        }
        billingCycles = Set.copyOf(EnumSet.copyOf(billingCycles));
    }

    public boolean isActive() { return status == PlanStatus.ACTIVE; }

    public boolean offers(BillingCycle cycle) { return billingCycles.contains(cycle); }

    public SubscriptionPlan revised(String newName, Set<BillingCycle> cycles, boolean newSelfService,
            Entitlements newEntitlements, Instant at) {
        return new SubscriptionPlan(id, code, newName, status, cycles, newSelfService, newEntitlements, createdAt, at);
    }

    public SubscriptionPlan retired(Instant at) {
        return new SubscriptionPlan(id, code, name, PlanStatus.RETIRED, billingCycles, selfService, entitlements, createdAt, at);
    }
}
