package com.buildingos.subscription.fee.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentRecord(UUID id, FeeCode feeCode, PaymentReference reference, Money paid,
        PaymentMethod method, String externalReference, LocalDate paidOn, UUID recordedBy, Instant recordedAt) {
    private static final int MAX_EXTERNAL_REFERENCE = 128;

    public PaymentRecord {
        if (paid.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (externalReference != null && externalReference.length() > MAX_EXTERNAL_REFERENCE) {
            throw new IllegalArgumentException("External reference is too long");
        }
    }
}
