package com.buildingos.subscription.subscription.application.getusersubscription;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.subscription.domain.model.Subscription;

public record UserSubscriptionView(Subscription subscription, Entitlements effectiveEntitlements) {}
