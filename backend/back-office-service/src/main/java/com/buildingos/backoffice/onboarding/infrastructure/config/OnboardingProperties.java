package com.buildingos.backoffice.onboarding.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.onboarding.*}: D-33d maximum session duration (default 30 days). */
@ConfigurationProperties("buildingos.onboarding")
public record OnboardingProperties(Duration maxDuration) {
    public OnboardingProperties {
        maxDuration = maxDuration == null ? Duration.ofDays(30) : maxDuration;
        if (maxDuration.isNegative() || maxDuration.isZero()) {
            throw new IllegalArgumentException("buildingos.onboarding.max-duration must be positive");
        }
    }
}
