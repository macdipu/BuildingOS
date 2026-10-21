package com.buildingos.backoffice.shared.infrastructure.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * This service's own token for directory lookups (D-38): exchanged at auth-service
 * {@code POST /internal/service-tokens} and cached until shortly before it expires.
 */
public final class ServiceTokenProvider {
    private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);
    private final RestClient auth;
    private final ServiceCredentialProperties credential;
    private final Clock clock;
    private String token;
    private Instant refreshAt = Instant.MIN;

    public ServiceTokenProvider(RestClient auth, ServiceCredentialProperties credential, Clock clock) {
        this.auth = auth;
        this.credential = credential;
        this.clock = clock;
    }

    public synchronized String token() {
        if (token == null || !clock.instant().isBefore(refreshAt)) {
            JsonNode body = auth.post().uri("/internal/service-tokens")
                    .body(Map.of("clientId", credential.clientId(), "clientSecret", credential.clientSecret()))
                    .retrieve().body(JsonNode.class);
            JsonNode data = body == null ? null : body.path("data");
            if (data == null || !data.path("accessToken").isString()) {
                throw new RestClientException("auth-service returned no service token");
            }
            long expiresIn = data.path("expiresIn").asLong(0);
            token = data.path("accessToken").asString();
            Instant expiry = clock.instant().plusSeconds(expiresIn);
            refreshAt = expiry.minus(REFRESH_MARGIN).isAfter(clock.instant()) ? expiry.minus(REFRESH_MARGIN) : expiry;
        }
        return token;
    }

    /** Drop the cached token, e.g. after a downstream 401, so the next call exchanges again. */
    public synchronized void invalidate() {
        token = null;
    }
}
