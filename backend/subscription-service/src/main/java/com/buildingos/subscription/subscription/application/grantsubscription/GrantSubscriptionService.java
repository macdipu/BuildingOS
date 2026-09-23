package com.buildingos.subscription.subscription.application.grantsubscription;

import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.subscription.application.SubscriptionStarter;
import com.buildingos.subscription.subscription.domain.model.GrantSource;
import com.buildingos.subscription.subscription.domain.model.Subscription;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;

/** Admin puts a user on any active plan (D-25); payment, if any, happens offline for now. */
public final class GrantSubscriptionService implements GrantSubscriptionUseCase {
    private final SubscriptionStarter starter;

    public GrantSubscriptionService(SubscriptionStarter starter) { this.starter = starter; }

    @Override
    public Subscription execute(Actor actor, GrantSubscriptionCommand command) {
        actor.requireRevenueAdmin();
        return starter.start(actor.userId(), SubscriptionSubject.user(command.userId()), command.planId(),
                command.billingCycle(), GrantSource.ADMIN);
    }
}
