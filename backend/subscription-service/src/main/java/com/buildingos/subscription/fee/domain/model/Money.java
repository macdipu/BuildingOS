package com.buildingos.subscription.fee.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

public record Money(BigDecimal amount, String currency) {
    private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");

    public Money {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 10) {
            throw new IllegalArgumentException("Amount must be non-negative with at most 2 decimals");
        }
        if (currency == null || !CURRENCY.matcher(currency).matches()) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
    }
}
