package com.buildingos.subscription.shared.infrastructure.config;

import com.buildingos.subscription.fee.application.getfeeschedule.GetFeeScheduleService;
import com.buildingos.subscription.fee.application.getfeeschedule.GetFeeScheduleUseCase;
import com.buildingos.subscription.fee.application.getfeestatus.GetFeeStatusService;
import com.buildingos.subscription.fee.application.getfeestatus.GetFeeStatusUseCase;
import com.buildingos.subscription.fee.application.recordpayment.RecordPaymentService;
import com.buildingos.subscription.fee.application.recordpayment.RecordPaymentUseCase;
import com.buildingos.subscription.fee.application.updatefeeschedule.UpdateFeeScheduleService;
import com.buildingos.subscription.fee.application.updatefeeschedule.UpdateFeeScheduleUseCase;
import com.buildingos.subscription.fee.domain.repository.FeeScheduleRepository;
import com.buildingos.subscription.fee.domain.repository.PaymentRecordRepository;
import com.buildingos.subscription.freetier.application.getfreetier.GetFreeTierService;
import com.buildingos.subscription.freetier.application.getfreetier.GetFreeTierUseCase;
import com.buildingos.subscription.freetier.application.updatefreetier.UpdateFreeTierService;
import com.buildingos.subscription.freetier.application.updatefreetier.UpdateFreeTierUseCase;
import com.buildingos.subscription.freetier.domain.repository.FreeTierRepository;
import com.buildingos.subscription.plan.application.createplan.CreatePlanService;
import com.buildingos.subscription.plan.application.createplan.CreatePlanUseCase;
import com.buildingos.subscription.plan.application.getplan.GetPlanService;
import com.buildingos.subscription.plan.application.getplan.GetPlanUseCase;
import com.buildingos.subscription.plan.application.listplans.ListPlansService;
import com.buildingos.subscription.plan.application.listplans.ListPlansUseCase;
import com.buildingos.subscription.plan.application.listselfserviceplans.ListSelfServicePlansService;
import com.buildingos.subscription.plan.application.listselfserviceplans.ListSelfServicePlansUseCase;
import com.buildingos.subscription.plan.application.retireplan.RetirePlanService;
import com.buildingos.subscription.plan.application.retireplan.RetirePlanUseCase;
import com.buildingos.subscription.plan.application.updateplan.UpdatePlanService;
import com.buildingos.subscription.plan.application.updateplan.UpdatePlanUseCase;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import com.buildingos.subscription.subscription.application.SubscriptionStarter;
import com.buildingos.subscription.subscription.application.entitlements.EntitlementResolver;
import com.buildingos.subscription.subscription.application.entitlements.FreeTierPlusSubscriptionResolver;
import com.buildingos.subscription.subscription.application.getmyentitlements.GetMyEntitlementsService;
import com.buildingos.subscription.subscription.application.getmyentitlements.GetMyEntitlementsUseCase;
import com.buildingos.subscription.subscription.application.getusersubscription.GetUserSubscriptionService;
import com.buildingos.subscription.subscription.application.getusersubscription.GetUserSubscriptionUseCase;
import com.buildingos.subscription.subscription.application.grantsubscription.GrantSubscriptionService;
import com.buildingos.subscription.subscription.application.grantsubscription.GrantSubscriptionUseCase;
import com.buildingos.subscription.subscription.application.selfsubscribe.SelfSubscribeService;
import com.buildingos.subscription.subscription.application.selfsubscribe.SelfSubscribeUseCase;
import com.buildingos.subscription.subscription.domain.repository.SubscriptionRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Bean wiring only. The entitlement resolver is the seam for a different revenue model (D-27). */
@Configuration(proxyBeanMethods = false)
public class RevenueConfiguration {
    @Bean
    Clock clock() { return Clock.systemUTC(); }

    @Bean
    EntitlementResolver entitlementResolver(FreeTierRepository freeTier, SubscriptionRepository subscriptions,
            SubscriptionPlanRepository plans) {
        return new FreeTierPlusSubscriptionResolver(freeTier, subscriptions, plans);
    }

    @Bean
    GetFreeTierUseCase getFreeTier(FreeTierRepository freeTier) { return new GetFreeTierService(freeTier); }

    @Bean
    UpdateFreeTierUseCase updateFreeTier(FreeTierRepository freeTier, AuditRecorder audit, UnitOfWork uow, Clock clock) {
        return new UpdateFreeTierService(freeTier, audit, uow, clock);
    }

    @Bean
    CreatePlanUseCase createPlan(SubscriptionPlanRepository plans, AuditRecorder audit, UnitOfWork uow, Clock clock) {
        return new CreatePlanService(plans, audit, uow, clock);
    }

    @Bean
    UpdatePlanUseCase updatePlan(SubscriptionPlanRepository plans, AuditRecorder audit, UnitOfWork uow, Clock clock) {
        return new UpdatePlanService(plans, audit, uow, clock);
    }

    @Bean
    RetirePlanUseCase retirePlan(SubscriptionPlanRepository plans, AuditRecorder audit, UnitOfWork uow, Clock clock) {
        return new RetirePlanService(plans, audit, uow, clock);
    }

    @Bean
    GetPlanUseCase getPlan(SubscriptionPlanRepository plans) { return new GetPlanService(plans); }

    @Bean
    ListPlansUseCase listPlans(SubscriptionPlanRepository plans) { return new ListPlansService(plans); }

    @Bean
    ListSelfServicePlansUseCase listSelfServicePlans(SubscriptionPlanRepository plans) {
        return new ListSelfServicePlansService(plans);
    }

    @Bean
    SubscriptionStarter subscriptionStarter(SubscriptionPlanRepository plans, SubscriptionRepository subscriptions,
            AuditRecorder audit, UnitOfWork uow, Clock clock) {
        return new SubscriptionStarter(plans, subscriptions, audit, uow, clock);
    }

    @Bean
    GrantSubscriptionUseCase grantSubscription(SubscriptionStarter starter) { return new GrantSubscriptionService(starter); }

    @Bean
    SelfSubscribeUseCase selfSubscribe(SubscriptionStarter starter) { return new SelfSubscribeService(starter); }

    @Bean
    GetUserSubscriptionUseCase getUserSubscription(SubscriptionRepository subscriptions, EntitlementResolver resolver) {
        return new GetUserSubscriptionService(subscriptions, resolver);
    }

    @Bean
    GetMyEntitlementsUseCase getMyEntitlements(EntitlementResolver resolver) {
        return new GetMyEntitlementsService(resolver);
    }

    @Bean
    GetFeeScheduleUseCase getFeeSchedule(FeeScheduleRepository schedules) { return new GetFeeScheduleService(schedules); }

    @Bean
    UpdateFeeScheduleUseCase updateFeeSchedule(FeeScheduleRepository schedules, AuditRecorder audit, UnitOfWork uow,
            Clock clock) {
        return new UpdateFeeScheduleService(schedules, audit, uow, clock);
    }

    @Bean
    RecordPaymentUseCase recordPayment(FeeScheduleRepository schedules, PaymentRecordRepository payments,
            AuditRecorder audit, UnitOfWork uow, Clock clock) {
        return new RecordPaymentService(schedules, payments, audit, uow, clock);
    }

    @Bean
    GetFeeStatusUseCase getFeeStatus(FeeScheduleRepository schedules, PaymentRecordRepository payments) {
        return new GetFeeStatusService(schedules, payments);
    }
}
