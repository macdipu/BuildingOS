package com.buildingos.subscription.fee.application.recordpayment;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.Money;
import com.buildingos.subscription.fee.domain.model.PaymentReference;
import java.time.LocalDate;

public record RecordPaymentCommand(FeeCode feeCode, PaymentReference reference, Money paid,
        String externalReference, LocalDate paidOn) {}
