package com.buildingos.subscription.fee.domain.model;

import java.math.BigDecimal;
import java.util.List;

public enum FeeStatus {
    SETTLED, UNPAID, NOT_REQUIRED;

    /** Settled once payments in the schedule currency add up to the scheduled amount. */
    public static FeeStatus of(FeeSchedule schedule, List<PaymentRecord> payments) {
        if (!schedule.required()) {
            return NOT_REQUIRED;
        }
        BigDecimal paid = payments.stream()
                .filter(payment -> payment.paid().currency().equals(schedule.price().currency()))
                .map(payment -> payment.paid().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return paid.compareTo(schedule.price().amount()) >= 0 ? SETTLED : UNPAID;
    }
}
