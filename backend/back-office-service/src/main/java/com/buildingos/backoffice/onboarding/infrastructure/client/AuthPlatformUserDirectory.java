package com.buildingos.backoffice.onboarding.infrastructure.client;

import com.buildingos.backoffice.onboarding.application.port.out.PlatformUserDirectory;
import com.buildingos.backoffice.shared.application.DependencyUnavailableException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/** auth-service {@code GET /internal/users/{id}} with the caller's relayed token (PlatformUserResponse). */
@Component
public class AuthPlatformUserDirectory implements PlatformUserDirectory {
    private static final String DEPENDENCY = "auth-service";
    private final RestClient client;

    public AuthPlatformUserDirectory(@Qualifier("authServiceClient") RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<Set<String>> platformRolesOf(UUID userId) {
        JsonNode body;
        try {
            body = client.get().uri("/internal/users/{id}", userId).retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound unknownUser) {
            return Optional.empty();
        } catch (RestClientException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
        JsonNode roles = body == null ? null : body.path("data").path("platformRoles");
        if (roles == null || !roles.isArray()) {
            throw new DependencyUnavailableException(DEPENDENCY,
                    new IllegalStateException("auth-service returned no platformRoles"));
        }
        Set<String> result = new HashSet<>();
        for (JsonNode role : roles) {
            result.add(role.asString());
        }
        return Optional.of(Set.copyOf(result));
    }
}
