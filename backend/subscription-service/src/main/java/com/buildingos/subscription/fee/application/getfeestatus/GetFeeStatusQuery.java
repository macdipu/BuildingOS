package com.buildingos.subscription.fee.application.getfeestatus;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.PaymentReference;

public record GetFeeStatusQuery(FeeCode feeCode, PaymentReference reference) {}
