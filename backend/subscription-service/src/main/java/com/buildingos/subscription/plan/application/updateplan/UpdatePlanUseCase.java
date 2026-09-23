package com.buildingos.subscription.plan.application.updateplan;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.shared.application.Actor;

public interface UpdatePlanUseCase {
    SubscriptionPlan execute(Actor actor, UpdatePlanCommand command);
}
