package com.buildingos.backoffice.supportsession.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.support.*}: D-36b maximum session duration (default 8 hours). */
@ConfigurationProperties("buildingos.support")
public record SupportProperties(Duration maxDuration) {
    public SupportProperties {
        maxDuration = maxDuration == null ? Duration.ofHours(8) : maxDuration;
        if (maxDuration.isNegative() || maxDuration.isZero()) {
            throw new IllegalArgumentException("buildingos.support.max-duration must be positive");
        }
    }
}
