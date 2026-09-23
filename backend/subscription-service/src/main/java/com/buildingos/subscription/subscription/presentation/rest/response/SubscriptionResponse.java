package com.buildingos.subscription.subscription.presentation.rest.response;

import com.buildingos.subscription.subscription.domain.model.Subscription;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubscriptionResponse(UUID id, UUID userId, UUID planId, String status, String billingCycle,
        String grantedBy, Instant startedAt, Map<String, Object> effectiveEntitlements) {
    public static SubscriptionResponse of(Subscription subscription, Map<String, Object> effectiveEntitlements) {
        return new SubscriptionResponse(subscription.id(), subscription.subject().id(), subscription.planId(),
                subscription.status().name(), subscription.billingCycle().name(), subscription.grantedBy().name(),
                subscription.startedAt(), effectiveEntitlements);
    }
}
