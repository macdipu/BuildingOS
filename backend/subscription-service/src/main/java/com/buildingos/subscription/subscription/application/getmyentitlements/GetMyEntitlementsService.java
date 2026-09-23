package com.buildingos.subscription.subscription.application.getmyentitlements;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.subscription.application.entitlements.EntitlementResolver;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;

/** Each user sees only their own entitlements (D-22). */
public final class GetMyEntitlementsService implements GetMyEntitlementsUseCase {
    private final EntitlementResolver entitlements;

    public GetMyEntitlementsService(EntitlementResolver entitlements) { this.entitlements = entitlements; }

    @Override
    public Entitlements execute(Actor actor) {
        return entitlements.resolve(SubscriptionSubject.user(actor.userId()));
    }
}
