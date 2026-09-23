package com.buildingos.subscription.subscription.application.getusersubscription;

import com.buildingos.subscription.shared.application.Actor;

public interface GetUserSubscriptionUseCase {
    UserSubscriptionView execute(Actor actor, GetUserSubscriptionQuery query);
}
