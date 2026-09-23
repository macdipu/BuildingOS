package com.buildingos.subscription.fee.presentation.rest.response;

import com.buildingos.subscription.fee.domain.model.PaymentRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(UUID id, String feeCode, String referenceType, UUID referenceId, BigDecimal amount,
        String currency, String method, String externalReference, LocalDate paidOn, UUID recordedBy,
        Instant recordedAt) {
    public static PaymentResponse of(PaymentRecord payment) {
        return new PaymentResponse(payment.id(), payment.feeCode().name(), payment.reference().type().name(),
                payment.reference().id(), payment.paid().amount(), payment.paid().currency(), payment.method().name(),
                payment.externalReference(), payment.paidOn(), payment.recordedBy(), payment.recordedAt());
    }
}
