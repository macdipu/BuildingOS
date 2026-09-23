package com.buildingos.subscription.plan.application.createplan;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.shared.application.Actor;

public interface CreatePlanUseCase {
    SubscriptionPlan execute(Actor actor, CreatePlanCommand command);
}
