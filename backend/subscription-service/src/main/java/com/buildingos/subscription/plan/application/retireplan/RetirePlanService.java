package com.buildingos.subscription.plan.application.retireplan;

import com.buildingos.subscription.plan.application.PlanAudit;
import com.buildingos.subscription.plan.application.PlanErrors;
import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import java.time.Clock;

/** One-way: a retired plan cannot be newly granted; existing subscriptions keep it. */
public final class RetirePlanService implements RetirePlanUseCase {
    private final SubscriptionPlanRepository plans;
    private final AuditRecorder audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RetirePlanService(SubscriptionPlanRepository plans, AuditRecorder audit, UnitOfWork unitOfWork, Clock clock) {
        this.plans = plans;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public SubscriptionPlan execute(Actor actor, RetirePlanCommand command) {
        actor.requireRevenueAdmin();
        return unitOfWork.inTransaction(() -> {
            var current = plans.findByIdForUpdate(command.planId())
                    .orElseThrow(() -> PlanErrors.notFound(command.planId()));
            if (!current.isActive()) {
                return current;
            }
            var now = clock.instant();
            SubscriptionPlan retired = current.retired(now);
            plans.update(retired);
            audit.record(new AuditRecorder.Entry(actor.userId(), "PLAN_RETIRED", "PLAN", current.id().toString(),
                    PlanAudit.snapshot(current), PlanAudit.snapshot(retired), now));
            return retired;
        });
    }
}
