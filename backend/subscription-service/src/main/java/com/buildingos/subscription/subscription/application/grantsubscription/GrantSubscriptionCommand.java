package com.buildingos.subscription.subscription.application.grantsubscription;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import java.util.UUID;

public record GrantSubscriptionCommand(UUID userId, UUID planId, BillingCycle billingCycle) {}
