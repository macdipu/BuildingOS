package com.buildingos.backoffice.auditlog.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.audit.*}: per-source timeout of the back-office audit fan-out (F6-T5d). */
@ConfigurationProperties("buildingos.audit")
public record AuditProperties(Duration timeout) {
    public AuditProperties {
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("buildingos.audit.timeout must be positive");
        }
    }
}
