package com.buildingos.subscription.plan.application.retireplan;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.shared.application.Actor;

public interface RetirePlanUseCase {
    SubscriptionPlan execute(Actor actor, RetirePlanCommand command);
}
