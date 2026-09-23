package com.buildingos.subscription.plan.application.getplan;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.shared.application.Actor;

public interface GetPlanUseCase {
    SubscriptionPlan execute(Actor actor, GetPlanQuery query);
}
