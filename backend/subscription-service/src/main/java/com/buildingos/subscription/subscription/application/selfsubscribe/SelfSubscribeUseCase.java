package com.buildingos.subscription.subscription.application.selfsubscribe;

import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.subscription.domain.model.Subscription;

public interface SelfSubscribeUseCase {
    Subscription execute(Actor actor, SelfSubscribeCommand command);
}
