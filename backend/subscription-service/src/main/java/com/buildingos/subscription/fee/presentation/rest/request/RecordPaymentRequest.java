package com.buildingos.subscription.fee.presentation.rest.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordPaymentRequest(String referenceType, UUID referenceId, BigDecimal amount, String currency,
        String externalReference, LocalDate paidOn) {}
