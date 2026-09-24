package com.buildingos.building.buildingapplication.domain.model;

import java.util.regex.Pattern;

/** Bangladesh mobile number in canonical local form {@code 01[3-9]XXXXXXXX} (same rule as auth-service, TASK-007). */
public record ContactPhone(String value) {
    private static final Pattern ACCEPTED = Pattern.compile("^(?:0|\\+?880)(1[3-9][0-9]{8})$");

    public ContactPhone {
        if (value == null || !value.matches("01[3-9][0-9]{8}")) {
            throw new IllegalArgumentException("Phone number must be a Bangladesh mobile number");
        }
    }

    /** Accepts {@code 01…}, {@code 8801…} or {@code +8801…}; digits only. */
    public static ContactPhone parse(String raw) {
        var matcher = ACCEPTED.matcher(raw == null ? "" : raw.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Phone number must be a Bangladesh mobile number");
        }
        return new ContactPhone("0" + matcher.group(1));
    }
}
