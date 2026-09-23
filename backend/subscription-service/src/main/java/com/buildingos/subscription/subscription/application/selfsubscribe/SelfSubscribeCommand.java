package com.buildingos.subscription.subscription.application.selfsubscribe;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import java.util.UUID;

public record SelfSubscribeCommand(UUID planId, BillingCycle billingCycle) {}
