package com.buildingos.subscription.fee.application.updatefeeschedule;

import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import com.buildingos.subscription.shared.application.Actor;

public interface UpdateFeeScheduleUseCase {
    FeeSchedule execute(Actor actor, UpdateFeeScheduleCommand command);
}
