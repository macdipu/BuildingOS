package com.buildingos.subscription.fee.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Operator-set amount and on/off switch for a one-time fee (D-24, D-27). Nothing is hardcoded. */
public record FeeSchedule(FeeCode code, Money price, boolean required, Instant updatedAt) {
    public FeeSchedule {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(price, "price");
    }
}
