package com.buildingos.building.shared.infrastructure.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.services.*}: direct (not via gateway) URLs of the services building-service calls. */
@ConfigurationProperties("buildingos.services")
public record ServiceClientProperties(String authUrl, String subscriptionUrl, Duration connectTimeout,
        Duration readTimeout) {
    public ServiceClientProperties {
        if (authUrl == null || authUrl.isBlank() || subscriptionUrl == null || subscriptionUrl.isBlank()) {
            throw new IllegalArgumentException("AUTH_SERVICE_URL and SUBSCRIPTION_SERVICE_URL are required");
        }
    }
}
