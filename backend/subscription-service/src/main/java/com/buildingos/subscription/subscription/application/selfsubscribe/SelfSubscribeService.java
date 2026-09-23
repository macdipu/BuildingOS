package com.buildingos.subscription.subscription.application.selfsubscribe;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.BusinessRuleException;
import com.buildingos.subscription.subscription.application.SubscriptionStarter;
import com.buildingos.subscription.subscription.domain.model.GrantSource;
import com.buildingos.subscription.subscription.domain.model.Subscription;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;

/** A user subscribes themselves to a self-service plan; free for now (D-25). */
public final class SelfSubscribeService implements SelfSubscribeUseCase {
    private final SubscriptionStarter starter;

    public SelfSubscribeService(SubscriptionStarter starter) { this.starter = starter; }

    @Override
    public Subscription execute(Actor actor, SelfSubscribeCommand command) {
        return starter.start(actor.userId(), SubscriptionSubject.user(actor.userId()), command.planId(),
                command.billingCycle(), GrantSource.SELF_SERVICE, SubscriptionPlan::selfService,
                () -> BusinessRuleException.forbidden("PLAN_NOT_SELF_SERVICE", "Plan is not available for self-service"));
    }
}
