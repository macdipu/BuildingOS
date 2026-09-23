package com.buildingos.subscription.subscription.application.entitlements;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.freetier.domain.repository.FreeTierRepository;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;
import com.buildingos.subscription.subscription.domain.repository.SubscriptionRepository;

/** Free tier for everyone (D-23), plus the subject's own active plan read live (D-20, D-22). */
public final class FreeTierPlusSubscriptionResolver implements EntitlementResolver {
    private final FreeTierRepository freeTier;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionPlanRepository plans;

    public FreeTierPlusSubscriptionResolver(FreeTierRepository freeTier, SubscriptionRepository subscriptions,
            SubscriptionPlanRepository plans) {
        this.freeTier = freeTier;
        this.subscriptions = subscriptions;
        this.plans = plans;
    }

    @Override
    public Entitlements resolve(SubscriptionSubject subject) {
        var base = freeTier.get();
        return subscriptions.findActive(subject)
                .flatMap(subscription -> plans.findById(subscription.planId()))
                .map(plan -> plan.entitlements().over(base))
                .orElse(base);
    }
}
