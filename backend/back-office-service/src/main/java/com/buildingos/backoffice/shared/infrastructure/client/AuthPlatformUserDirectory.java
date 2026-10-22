package com.buildingos.backoffice.shared.infrastructure.client;

import com.buildingos.backoffice.shared.application.port.out.PlatformUserDirectory;
import com.buildingos.backoffice.shared.application.DependencyUnavailableException;
import com.buildingos.backoffice.shared.application.NotPermittedException;
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

/** auth-service {@code GET /internal/directory/users/{id}} with this service's own token (D-38). */
@Component
public class AuthPlatformUserDirectory implements PlatformUserDirectory {
    private static final String DEPENDENCY = "auth-service";
    private final RestClient client;

    public AuthPlatformUserDirectory(@Qualifier("authDirectoryClient") RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<Set<String>> platformRolesOf(UUID userId) {
        JsonNode body;
        try {
            body = client.get().uri("/internal/directory/users/{id}", userId).retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound unknownUser) {
            return Optional.empty();
        } catch (HttpClientErrorException.Forbidden denied) {
            // D-38: a refused lookup is a 403, not an outage.
            throw new NotPermittedException();
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
