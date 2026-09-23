package com.buildingos.subscription.plan.application.updateplan;

import com.buildingos.subscription.plan.application.PlanAudit;
import com.buildingos.subscription.plan.application.PlanErrors;
import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import java.time.Clock;

/** Edits apply immediately to every subscriber of the plan (D-20). */
public final class UpdatePlanService implements UpdatePlanUseCase {
    private final SubscriptionPlanRepository plans;
    private final AuditRecorder audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UpdatePlanService(SubscriptionPlanRepository plans, AuditRecorder audit, UnitOfWork unitOfWork, Clock clock) {
        this.plans = plans;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public SubscriptionPlan execute(Actor actor, UpdatePlanCommand command) {
        actor.requireRevenueAdmin();
        return unitOfWork.inTransaction(() -> {
            var current = plans.findByIdForUpdate(command.planId())
                    .orElseThrow(() -> PlanErrors.notFound(command.planId()));
            if (!current.isActive()) {
                throw PlanErrors.retired();
            }
            var now = clock.instant();
            SubscriptionPlan revised = current.revised(command.name(), command.billingCycles(),
                    command.selfService(), command.entitlements(), now);
            plans.update(revised);
            audit.record(new AuditRecorder.Entry(actor.userId(), "PLAN_UPDATED", "PLAN", current.id().toString(),
                    PlanAudit.snapshot(current), PlanAudit.snapshot(revised), now));
            return revised;
        });
    }
}
