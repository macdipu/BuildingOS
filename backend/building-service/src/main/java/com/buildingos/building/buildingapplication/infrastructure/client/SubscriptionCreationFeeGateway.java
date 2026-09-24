package com.buildingos.building.buildingapplication.infrastructure.client;

import com.buildingos.building.buildingapplication.application.port.out.CreationFeeGateway;
import com.buildingos.building.buildingapplication.application.port.out.CreationFeeStatus;
import com.buildingos.building.shared.application.BusinessRuleException;
import com.buildingos.building.shared.application.DependencyUnavailableException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

/** subscription-service {@code GET /api/v1/platform/fees/BUILDING_CREATION/status} (F5a TECH-SPEC §5). */
@Component
public class SubscriptionCreationFeeGateway implements CreationFeeGateway {
    private static final String DEPENDENCY = "subscription-service";
    private final RestClient client;

    public SubscriptionCreationFeeGateway(@Qualifier("subscriptionServiceClient") RestClient client) {
        this.client = client;
    }

    @Override
    public CreationFeeStatus status(UUID applicationId) {
        JsonNode body;
        try {
            body = client.get()
                    .uri(uri -> uri.path("/api/v1/platform/fees/BUILDING_CREATION/status")
                            .queryParam("referenceType", "BUILDING_APPLICATION")
                            .queryParam("referenceId", applicationId).build())
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException rejected) {
            if (rejected.getStatusCode().value() == 409
                    && rejected.getResponseBodyAsString().contains("\"FEE_NOT_CONFIGURED\"")) {
                throw BusinessRuleException.conflict("FEE_NOT_CONFIGURED",
                        "The building-creation fee is not configured yet");
            }
            throw new DependencyUnavailableException(DEPENDENCY, rejected);
        } catch (RestClientException unreachable) {
            throw new DependencyUnavailableException(DEPENDENCY, unreachable);
        }
        try {
            return CreationFeeStatus.valueOf(body.path("data").path("status").asString());
        } catch (IllegalArgumentException | NullPointerException unexpected) {
            throw new DependencyUnavailableException(DEPENDENCY, unexpected);
        }
    }
}
