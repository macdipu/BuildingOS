package com.buildingos.subscription.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.catalog.domain.model.InvalidEntitlementsException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EntitlementsTest {
    @Test
    void acceptsEveryCatalogTypeAndRoundTrips() {
        var raw = Map.<String, Object>of("maintenance.enabled", true, "rent_management.enabled", false,
                "max_units", 20, "storage_limit_mb", 1024L, "support_tier", "standard");
        assertThat(Entitlements.fromKeys(raw).toKeys()).containsEntry("max_units", 20L)
                .containsEntry("storage_limit_mb", 1024L).containsEntry("support_tier", "standard")
                .containsEntry("maintenance.enabled", true).hasSize(5);
    }

    @Test
    void rejectsUnknownKeysWrongTypesAndBadValues() {
        for (var bad : java.util.List.<Map<String, Object>>of(
                Map.of("unknown.feature", true), Map.of("maintenance.enabled", "yes"),
                Map.of("max_units", -1), Map.of("max_units", 2.5), Map.of("max_units", true),
                Map.of("support_tier", " "), Map.of("support_tier", "x".repeat(65)))) {
            assertThatThrownBy(() -> Entitlements.fromKeys(bad)).isInstanceOf(InvalidEntitlementsException.class);
        }
        var nullValue = new HashMap<String, Object>();
        nullValue.put("maintenance.enabled", null);
        assertThatThrownBy(() -> Entitlements.fromKeys(nullValue)).isInstanceOf(InvalidEntitlementsException.class);
        assertThatThrownBy(() -> Entitlements.fromKeys(null)).isInstanceOf(InvalidEntitlementsException.class);
    }

    @Test
    void planLayeredOverFreeTierOrsFlagsAndOverridesLimits() {
        var free = Entitlements.fromKeys(Map.of("maintenance.enabled", true, "max_units", 5));
        var plan = Entitlements.fromKeys(Map.of("maintenance.enabled", false, "rent_management.enabled", true,
                "max_units", 50));
        assertThat(plan.over(free).toKeys()).containsEntry("maintenance.enabled", true)
                .containsEntry("rent_management.enabled", true).containsEntry("max_units", 50L);
        assertThat(Entitlements.none().over(free)).isEqualTo(free);
    }
}
