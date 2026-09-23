package com.buildingos.subscription.plan.application.listselfserviceplans;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.Actor;
import java.util.List;

/** Plans any logged-in user may pick in the app: active and marked self-service. */
public final class ListSelfServicePlansService implements ListSelfServicePlansUseCase {
    private final SubscriptionPlanRepository plans;

    public ListSelfServicePlansService(SubscriptionPlanRepository plans) { this.plans = plans; }

    @Override
    public List<SubscriptionPlan> execute(Actor actor) {
        return plans.findAll().stream().filter(plan -> plan.isActive() && plan.selfService()).toList();
    }
}
