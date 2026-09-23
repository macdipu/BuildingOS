package com.buildingos.subscription.plan.domain.model;

import java.util.regex.Pattern;

/** Stable plan identifier chosen by the operator, e.g. {@code PREMIUM_MONTHLY}. Immutable once created. */
public record PlanCode(String value) {
    private static final Pattern FORMAT = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    public PlanCode {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Plan code must be 2-64 characters of A-Z, 0-9 or _ starting with a letter");
        }
    }
}
