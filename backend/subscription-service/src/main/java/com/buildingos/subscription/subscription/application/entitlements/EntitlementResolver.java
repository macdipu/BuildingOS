package com.buildingos.subscription.subscription.application.entitlements;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;

/** Effective entitlements for a subject. Swap the implementation to change the revenue model (D-27). */
public interface EntitlementResolver {
    Entitlements resolve(SubscriptionSubject subject);
}
