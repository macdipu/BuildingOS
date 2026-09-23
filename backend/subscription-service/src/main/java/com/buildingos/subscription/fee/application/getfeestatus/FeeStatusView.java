package com.buildingos.subscription.fee.application.getfeestatus;

import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import com.buildingos.subscription.fee.domain.model.FeeStatus;
import com.buildingos.subscription.fee.domain.model.PaymentRecord;
import java.util.List;

public record FeeStatusView(FeeStatus status, FeeSchedule schedule, List<PaymentRecord> payments) {}
