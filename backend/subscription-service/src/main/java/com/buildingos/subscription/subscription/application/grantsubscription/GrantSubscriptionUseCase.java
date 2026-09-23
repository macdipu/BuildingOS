package com.buildingos.subscription.subscription.application.grantsubscription;

import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.subscription.domain.model.Subscription;

public interface GrantSubscriptionUseCase {
    Subscription execute(Actor actor, GrantSubscriptionCommand command);
}
