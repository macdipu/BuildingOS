package com.buildingos.subscription.subscription.application.listplatformsubscriptions;

import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.subscription.domain.repository.SubscriptionRepository;

/** Back-office user subscription list (D-35, per-user model D-22), newest first. */
public final class ListPlatformSubscriptionsService implements ListPlatformSubscriptionsUseCase {
    private final SubscriptionRepository subscriptions;

    public ListPlatformSubscriptionsService(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public PlatformSubscriptionPage execute(Actor actor, ListPlatformSubscriptionsQuery query) {
        actor.requireRevenueAdmin();
        if (query.page() < 0 || query.size() < 1 || query.size() > PlatformSubscriptionPage.MAX_SIZE) {
            throw new IllegalArgumentException("page must be >= 0 and size 1-" + PlatformSubscriptionPage.MAX_SIZE);
        }
        if ((long) query.page() * query.size() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("page is too large");
        }
        String plan = query.planCode() == null || query.planCode().isBlank() ? null : query.planCode().trim();
        var items = subscriptions.findPage(query.status(), plan, query.page() * query.size(), query.size());
        return new PlatformSubscriptionPage(items, query.page(), query.size(), subscriptions.count(query.status(), plan));
    }
}
