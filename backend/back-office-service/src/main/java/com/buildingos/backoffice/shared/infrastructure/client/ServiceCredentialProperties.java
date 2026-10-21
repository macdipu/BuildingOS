package com.buildingos.backoffice.shared.infrastructure.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.service-credential.*}: this service's own client credentials at auth-service (D-38). */
@ConfigurationProperties("buildingos.service-credential")
public record ServiceCredentialProperties(String clientId, String clientSecret) {
    public ServiceCredentialProperties {
        clientId = clientId == null || clientId.isBlank() ? "back-office-service" : clientId;
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("BACKOFFICE_SERVICE_CLIENT_SECRET is required");
        }
    }

    @Override
    public String toString() {
        return "ServiceCredentialProperties[clientId=" + clientId + "]";
    }
}
