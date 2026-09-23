package com.buildingos.subscription.subscription.application;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.plan.application.PlanErrors;
import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.BusinessRuleException;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import com.buildingos.subscription.subscription.domain.model.GrantSource;
import com.buildingos.subscription.subscription.domain.model.Subscription;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;
import com.buildingos.subscription.subscription.domain.repository.SubscriptionRepository;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Shared start rules for admin grants and self-subscribe: active plan, offered cycle, one active per subject. */
public final class SubscriptionStarter {
    private final SubscriptionPlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final AuditRecorder audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public SubscriptionStarter(SubscriptionPlanRepository plans, SubscriptionRepository subscriptions,
            AuditRecorder audit, UnitOfWork unitOfWork, Clock clock) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    /** Any active plan (admin grant). */
    public Subscription start(UUID actorUserId, SubscriptionSubject subject, UUID planId, BillingCycle cycle,
            GrantSource source) {
        return start(actorUserId, subject, planId, cycle, source, plan -> true, () -> null);
    }

    /** Only plans passing {@code planAllowed}; otherwise {@code planNotAllowed} is thrown. */
    public Subscription start(UUID actorUserId, SubscriptionSubject subject, UUID planId, BillingCycle cycle,
            GrantSource source, Predicate<SubscriptionPlan> planAllowed, Supplier<BusinessRuleException> planNotAllowed) {
        return unitOfWork.inTransaction(() -> {
            var plan = plans.findByIdForUpdate(planId).orElseThrow(() -> PlanErrors.notFound(planId));
            if (!plan.isActive()) {
                throw PlanErrors.retired();
            }
            if (!planAllowed.test(plan)) {
                throw planNotAllowed.get();
            }
            if (!plan.offers(cycle)) {
                throw BusinessRuleException.conflict("BILLING_CYCLE_NOT_OFFERED", "Plan does not offer " + cycle);
            }
            var now = clock.instant();
            var subscription = Subscription.start(subject, planId, cycle, source, now);
            if (!subscriptions.insertIfNoneActive(subscription)) {
                throw BusinessRuleException.conflict("ALREADY_SUBSCRIBED", "An active subscription already exists");
            }
            audit.record(new AuditRecorder.Entry(actorUserId, "SUBSCRIPTION_STARTED", "SUBSCRIPTION",
                    subscription.id().toString(), null, Map.of(
                            "subjectType", subject.type().name(), "subjectId", subject.id().toString(),
                            "planId", planId.toString(), "billingCycle", cycle.name(), "grantedBy", source.name()),
                    now));
            return subscription;
        });
    }
}
