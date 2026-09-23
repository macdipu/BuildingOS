package com.buildingos.subscription.catalog.domain.model;

import java.util.Arrays;
import java.util.Optional;

/** Catalog of entitlement keys (BRD §149.16, D-16). A new key is one new constant. */
public enum Feature {
    MAINTENANCE("maintenance.enabled", Kind.FLAG),
    RENT_MANAGEMENT("rent_management.enabled", Kind.FLAG),
    WORK_ORDERS("work_orders.enabled", Kind.FLAG),
    REPORTS_PDF_EXPORT("reports.pdf_export", Kind.FLAG),
    REPORTS_EXCEL_EXPORT("reports.excel_export", Kind.FLAG),
    OFFLINE_SYNC("offline_sync.enabled", Kind.FLAG),
    MAX_UNITS("max_units", Kind.LIMIT),
    MAX_USERS("max_users", Kind.LIMIT),
    STORAGE_LIMIT_MB("storage_limit_mb", Kind.LIMIT),
    SUPPORT_TIER("support_tier", Kind.TEXT);

    public enum Kind { FLAG, LIMIT, TEXT }

    private final String key;
    private final Kind kind;

    Feature(String key, Kind kind) {
        this.key = key;
        this.kind = kind;
    }

    public String key() { return key; }
    public Kind kind() { return kind; }

    public static Optional<Feature> byKey(String key) {
        return Arrays.stream(values()).filter(feature -> feature.key.equals(key)).findFirst();
    }
}
