package com.buildingos.building.ownership.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Ownership percentage: exact, >0, ≤100, at most 4 decimals; extra precision is rejected (UO-D03). */
public record Share(BigDecimal percent) implements Comparable<Share> {
    public static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;

    public Share {
        Objects.requireNonNull(percent, "percent");
        if (percent.signum() <= 0) {
            throw OwnershipRuleException.invalid("SHARE_NOT_POSITIVE", "share must be greater than 0");
        }
        if (percent.compareTo(HUNDRED) > 0) {
            throw OwnershipRuleException.invalid("SHARE_TOO_LARGE", "share must be at most 100");
        }
        if (percent.stripTrailingZeros().scale() > SCALE) {
            throw OwnershipRuleException.invalid("SHARE_PRECISION", "share allows at most 4 decimals");
        }
        percent = percent.setScale(SCALE);
    }

    public static Share of(BigDecimal percent) {
        if (percent == null) {
            throw OwnershipRuleException.invalid("SHARE_REQUIRED", "share is required");
        }
        return new Share(percent);
    }

    public Share plus(Share other) { return new Share(percent.add(other.percent)); }

    /** Remaining share after moving {@code other} away, or null when nothing remains. */
    public Share minusOrNull(Share other) {
        var rest = percent.subtract(other.percent);
        return rest.signum() == 0 ? null : new Share(rest);
    }

    @Override
    public int compareTo(Share other) { return percent.compareTo(other.percent); }
}
