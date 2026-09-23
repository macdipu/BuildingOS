package com.buildingos.subscription.plan.application.listplans;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.Actor;
import java.util.List;

public final class ListPlansService implements ListPlansUseCase {
    private final SubscriptionPlanRepository plans;

    public ListPlansService(SubscriptionPlanRepository plans) { this.plans = plans; }

    @Override
    public List<SubscriptionPlan> execute(Actor actor) {
        actor.requireRevenueAdmin();
        return plans.findAll();
    }
}
