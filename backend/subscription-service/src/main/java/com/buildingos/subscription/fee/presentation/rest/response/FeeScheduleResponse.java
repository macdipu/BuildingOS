package com.buildingos.subscription.fee.presentation.rest.response;

import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import java.math.BigDecimal;
import java.time.Instant;

public record FeeScheduleResponse(String code, BigDecimal amount, String currency, boolean required, Instant updatedAt) {
    public static FeeScheduleResponse of(FeeSchedule schedule) {
        return new FeeScheduleResponse(schedule.code().name(), schedule.price().amount(), schedule.price().currency(),
                schedule.required(), schedule.updatedAt());
    }
}
