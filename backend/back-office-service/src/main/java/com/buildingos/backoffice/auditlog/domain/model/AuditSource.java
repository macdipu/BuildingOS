package com.buildingos.backoffice.auditlog.domain.model;

import java.util.Arrays;

/** Services whose audit rows the back-office audit view aggregates (F6-T5d, D-37), by wire name. */
public enum AuditSource {
    AUTH_SERVICE("auth-service"),
    BUILDING_SERVICE("building-service"),
    SUBSCRIPTION_SERVICE("subscription-service"),
    BACK_OFFICE_SERVICE("back-office-service");

    private final String wireName;

    AuditSource(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return wireName;
    }

    /** Unknown name is an {@link IllegalArgumentException} (400 at the edge). */
    public static AuditSource fromWireName(String name) {
        return Arrays.stream(values()).filter(source -> source.wireName.equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("source has an unknown value: " + name));
    }
}
