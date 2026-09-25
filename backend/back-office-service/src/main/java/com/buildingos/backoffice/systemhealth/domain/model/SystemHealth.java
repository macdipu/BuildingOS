package com.buildingos.backoffice.systemhealth.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Point-in-time readiness of every monitored service with the rolled-up overall status. */
public record SystemHealth(Instant checkedAt, OverallHealth overall, List<ServiceReadiness> services) {
    public SystemHealth {
        Objects.requireNonNull(checkedAt, "checkedAt");
        services = List.copyOf(services);
        Objects.requireNonNull(overall, "overall");
    }

    public static SystemHealth of(Instant checkedAt, List<ServiceReadiness> services) {
        return new SystemHealth(checkedAt,
                OverallHealth.of(services.stream().map(ServiceReadiness::status).toList()), services);
    }
}
