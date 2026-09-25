package com.buildingos.backoffice.shared.infrastructure.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.services.*}: direct (not via gateway) URLs of the services back-office-service calls. */
@ConfigurationProperties("buildingos.services")
public record ServiceClientProperties(String authUrl, String buildingUrl, String subscriptionUrl,
        Duration connectTimeout, Duration readTimeout) {
    public ServiceClientProperties {
        if (blank(authUrl) || blank(buildingUrl) || blank(subscriptionUrl)) {
            throw new IllegalArgumentException(
                    "AUTH_SERVICE_URL, BUILDING_SERVICE_URL and SUBSCRIPTION_SERVICE_URL are required");
        }
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
