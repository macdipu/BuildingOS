package com.buildingos.subscription.plan.application.createplan;

import com.buildingos.subscription.plan.application.PlanAudit;
import com.buildingos.subscription.plan.domain.model.PlanStatus;
import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.BusinessRuleException;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import java.time.Clock;
import java.util.UUID;

public final class CreatePlanService implements CreatePlanUseCase {
    private final SubscriptionPlanRepository plans;
    private final AuditRecorder audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public CreatePlanService(SubscriptionPlanRepository plans, AuditRecorder audit, UnitOfWork unitOfWork, Clock clock) {
        this.plans = plans;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public SubscriptionPlan execute(Actor actor, CreatePlanCommand command) {
        actor.requireRevenueAdmin();
        var now = clock.instant();
        var plan = new SubscriptionPlan(UUID.randomUUID(), command.code(), command.name(), PlanStatus.ACTIVE,
                command.billingCycles(), command.selfService(), command.entitlements(), now, now);
        return unitOfWork.inTransaction(() -> {
            if (!plans.insert(plan)) {
                throw BusinessRuleException.conflict("PLAN_CODE_TAKEN", "Plan code already exists");
            }
            audit.record(new AuditRecorder.Entry(actor.userId(), "PLAN_CREATED", "PLAN", plan.id().toString(),
                    null, PlanAudit.snapshot(plan), now));
            return plan;
        });
    }
}
