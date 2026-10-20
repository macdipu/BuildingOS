package com.buildingos.subscription.subscription.application.listplatformsubscriptions;

import com.buildingos.subscription.shared.application.Actor;

public interface ListPlatformSubscriptionsUseCase {
    PlatformSubscriptionPage execute(Actor actor, ListPlatformSubscriptionsQuery query);
}
