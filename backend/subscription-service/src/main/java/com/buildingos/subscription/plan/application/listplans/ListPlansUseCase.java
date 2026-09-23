package com.buildingos.subscription.plan.application.listplans;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.shared.application.Actor;
import java.util.List;

public interface ListPlansUseCase {
    List<SubscriptionPlan> execute(Actor actor);
}
