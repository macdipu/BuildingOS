package com.buildingos.backoffice.shared.infrastructure.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.services.*}: direct (not via gateway) URLs of the services back-office-service calls. */
@ConfigurationProperties("buildingos.services")
public record ServiceClientProperties(String authUrl, String buildingUrl, Duration connectTimeout,
        Duration readTimeout) {
    public ServiceClientProperties {
        if (authUrl == null || authUrl.isBlank() || buildingUrl == null || buildingUrl.isBlank()) {
            throw new IllegalArgumentException("AUTH_SERVICE_URL and BUILDING_SERVICE_URL are required");
        }
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
    }
}
