package com.buildingos.subscription.fee.application.getfeeschedule;

import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import com.buildingos.subscription.shared.application.Actor;

public interface GetFeeScheduleUseCase {
    FeeSchedule execute(Actor actor, GetFeeScheduleQuery query);
}
