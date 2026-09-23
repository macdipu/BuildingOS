package com.buildingos.subscription.subscription.application.getusersubscription;

import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.BusinessRuleException;
import com.buildingos.subscription.subscription.application.entitlements.EntitlementResolver;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;
import com.buildingos.subscription.subscription.domain.repository.SubscriptionRepository;

public final class GetUserSubscriptionService implements GetUserSubscriptionUseCase {
    private final SubscriptionRepository subscriptions;
    private final EntitlementResolver entitlements;

    public GetUserSubscriptionService(SubscriptionRepository subscriptions, EntitlementResolver entitlements) {
        this.subscriptions = subscriptions;
        this.entitlements = entitlements;
    }

    @Override
    public UserSubscriptionView execute(Actor actor, GetUserSubscriptionQuery query) {
        actor.requireRevenueAdmin();
        var subject = SubscriptionSubject.user(query.userId());
        var subscription = subscriptions.findActive(subject).orElseThrow(() ->
                BusinessRuleException.notFound("SUBSCRIPTION_NOT_FOUND", "User has no active subscription"));
        return new UserSubscriptionView(subscription, entitlements.resolve(subject));
    }
}
