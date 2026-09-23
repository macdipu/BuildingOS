package com.buildingos.subscription.subscription.domain.model;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import java.time.Instant;
import java.util.UUID;

public record Subscription(UUID id, SubscriptionSubject subject, UUID planId, SubscriptionStatus status,
        BillingCycle billingCycle, GrantSource grantedBy, Instant startedAt) {
    public static Subscription start(SubscriptionSubject subject, UUID planId, BillingCycle cycle,
            GrantSource source, Instant at) {
        return new Subscription(UUID.randomUUID(), subject, planId, SubscriptionStatus.ACTIVE, cycle, source, at);
    }
}
