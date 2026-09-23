package com.buildingos.subscription.plan.application.getplan;

import com.buildingos.subscription.plan.application.PlanErrors;
import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.Actor;

public final class GetPlanService implements GetPlanUseCase {
    private final SubscriptionPlanRepository plans;

    public GetPlanService(SubscriptionPlanRepository plans) { this.plans = plans; }

    @Override
    public SubscriptionPlan execute(Actor actor, GetPlanQuery query) {
        actor.requireRevenueAdmin();
        return plans.findById(query.planId()).orElseThrow(() -> PlanErrors.notFound(query.planId()));
    }
}
