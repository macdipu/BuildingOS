package com.buildingos.subscription.fee.presentation.rest.response;

import com.buildingos.subscription.fee.application.getfeestatus.FeeStatusView;
import java.util.List;

public record FeeStatusResponse(String status, FeeScheduleResponse schedule, List<PaymentResponse> payments) {
    public static FeeStatusResponse of(FeeStatusView view) {
        return new FeeStatusResponse(view.status().name(), FeeScheduleResponse.of(view.schedule()),
                view.payments().stream().map(PaymentResponse::of).toList());
    }
}
