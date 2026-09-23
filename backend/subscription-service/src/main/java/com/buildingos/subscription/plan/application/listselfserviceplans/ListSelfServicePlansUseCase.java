package com.buildingos.subscription.plan.application.listselfserviceplans;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.shared.application.Actor;
import java.util.List;

public interface ListSelfServicePlansUseCase {
    List<SubscriptionPlan> execute(Actor actor);
}
