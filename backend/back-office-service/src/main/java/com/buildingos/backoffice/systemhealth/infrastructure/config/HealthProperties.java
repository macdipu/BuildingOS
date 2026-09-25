package com.buildingos.backoffice.systemhealth.infrastructure.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code buildingos.health.*}: services whose {@code /actuator/health/readiness} System Health aggregates (BOC-08),
 * and the per-call timeout. An entry without {@code url} is back-office-service itself.
 */
@ConfigurationProperties("buildingos.health")
public record HealthProperties(Duration timeout, List<MonitoredService> services) {
    public HealthProperties {
        timeout = timeout == null ? Duration.ofSeconds(2) : timeout;
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("buildingos.health.timeout must be positive");
        }
        services = services == null ? List.of() : List.copyOf(services);
        if (services.stream().anyMatch(service -> service.name() == null || service.name().isBlank())) {
            throw new IllegalArgumentException("every buildingos.health.services entry needs a name");
        }
    }

    public record MonitoredService(String name, String url) {
        public boolean isSelf() {
            return url == null || url.isBlank();
        }
    }
}
