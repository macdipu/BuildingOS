package com.buildingos.subscription.fee.application.getfeeschedule;

import com.buildingos.subscription.fee.application.FeeErrors;
import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import com.buildingos.subscription.fee.domain.repository.FeeScheduleRepository;
import com.buildingos.subscription.shared.application.Actor;

public final class GetFeeScheduleService implements GetFeeScheduleUseCase {
    private final FeeScheduleRepository schedules;

    public GetFeeScheduleService(FeeScheduleRepository schedules) { this.schedules = schedules; }

    @Override
    public FeeSchedule execute(Actor actor, GetFeeScheduleQuery query) {
        actor.requireRevenueAdmin();
        return schedules.find(query.code()).orElseThrow(() -> FeeErrors.notConfigured(query.code()));
    }
}
