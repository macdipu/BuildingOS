package com.buildingos.subscription.catalog.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validated entitlement values keyed by {@link Feature}: flags are booleans, limits are
 * non-negative whole numbers (absent = unlimited), text is a short non-blank string.
 */
public record Entitlements(Map<Feature, Object> values) {
    private static final int MAX_TEXT_LENGTH = 64;

    public Entitlements {
        var copy = new EnumMap<Feature, Object>(Feature.class);
        values.forEach((feature, value) -> copy.put(feature, validated(feature, value)));
        values = Collections.unmodifiableMap(copy);
    }

    public static Entitlements none() {
        return new Entitlements(Map.of());
    }

    /** Parses wire/storage form ({@code "max_units": 20}); unknown keys or wrong types are rejected. */
    public static Entitlements fromKeys(Map<String, ?> raw) {
        if (raw == null) {
            throw new InvalidEntitlementsException("Entitlements are required");
        }
        var parsed = new EnumMap<Feature, Object>(Feature.class);
        raw.forEach((key, value) -> parsed.put(Feature.byKey(key)
                .orElseThrow(() -> new InvalidEntitlementsException("Unknown entitlement: " + key)), value));
        return new Entitlements(parsed);
    }

    public Map<String, Object> toKeys() {
        var keys = new LinkedHashMap<String, Object>();
        values.forEach((feature, value) -> keys.put(feature.key(), value));
        return keys;
    }

    public boolean contains(Feature feature) {
        return values.containsKey(feature);
    }

    /** Layers {@code this} over {@code base}: flags are OR-ed; limits and text from {@code this} win when present. */
    public Entitlements over(Entitlements base) {
        var merged = new EnumMap<Feature, Object>(Feature.class);
        merged.putAll(base.values);
        values.forEach((feature, value) -> merged.merge(feature, value, (older, newer) ->
                feature.kind() == Feature.Kind.FLAG ? (Boolean) older || (Boolean) newer : newer));
        return new Entitlements(merged);
    }

    private static Object validated(Feature feature, Object value) {
        return switch (feature.kind()) {
            case FLAG -> {
                if (!(value instanceof Boolean flag)) {
                    throw new InvalidEntitlementsException(feature.key() + " must be true or false");
                }
                yield flag;
            }
            case LIMIT -> {
                if (!(value instanceof Integer || value instanceof Long) || ((Number) value).longValue() < 0) {
                    throw new InvalidEntitlementsException(feature.key() + " must be a non-negative whole number");
                }
                yield ((Number) value).longValue();
            }
            case TEXT -> {
                if (!(value instanceof String text) || text.isBlank() || text.length() > MAX_TEXT_LENGTH) {
                    throw new InvalidEntitlementsException(feature.key() + " must be 1-" + MAX_TEXT_LENGTH + " characters");
                }
                yield text;
            }
        };
    }
}
