package com.buildingos.subscription.fee.application.getfeestatus;

import com.buildingos.subscription.fee.application.FeeErrors;
import com.buildingos.subscription.fee.domain.model.FeeStatus;
import com.buildingos.subscription.fee.domain.repository.FeeScheduleRepository;
import com.buildingos.subscription.fee.domain.repository.PaymentRecordRepository;
import com.buildingos.subscription.shared.application.Actor;

/** Answers "is this fee settled?" — the building-application approval precondition (D-26). */
public final class GetFeeStatusService implements GetFeeStatusUseCase {
    private final FeeScheduleRepository schedules;
    private final PaymentRecordRepository payments;

    public GetFeeStatusService(FeeScheduleRepository schedules, PaymentRecordRepository payments) {
        this.schedules = schedules;
        this.payments = payments;
    }

    @Override
    public FeeStatusView execute(Actor actor, GetFeeStatusQuery query) {
        actor.requireRevenueAdmin();
        var schedule = schedules.find(query.feeCode()).orElseThrow(() -> FeeErrors.notConfigured(query.feeCode()));
        var recorded = payments.find(query.feeCode(), query.reference());
        return new FeeStatusView(FeeStatus.of(schedule, recorded), schedule, recorded);
    }
}
