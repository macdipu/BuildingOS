package com.buildingos.building.buildingapplication.infrastructure.client;

import com.buildingos.building.buildingapplication.application.port.out.UserProvisioning;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.shared.application.DependencyUnavailableException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/** auth-service {@code POST /internal/users/provision} (D-29). */
@Component
public class AuthUserProvisioning implements UserProvisioning {
    private static final String DEPENDENCY = "auth-service";
    private final RestClient client;

    public AuthUserProvisioning(@Qualifier("authServiceClient") RestClient client) {
        this.client = client;
    }

    @Override
    public UUID provision(ContactPhone phone) {
        try {
            JsonNode body = client.post().uri("/internal/users/provision").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("phone", phone.value())).retrieve().body(JsonNode.class);
            return UUID.fromString(body.path("data").path("userId").asString());
        } catch (RestClientException | IllegalArgumentException | NullPointerException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
    }
}
