package com.buildingos.backoffice.onboarding.infrastructure.client;

import com.buildingos.backoffice.onboarding.application.port.out.BuildingDirectory;
import com.buildingos.backoffice.shared.application.DependencyUnavailableException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** building-service {@code GET /api/v1/platform/buildings/{id}} with the caller's relayed token. */
@Component
public class BuildingServiceBuildingDirectory implements BuildingDirectory {
    private static final String DEPENDENCY = "building-service";
    private final RestClient client;

    public BuildingServiceBuildingDirectory(@Qualifier("buildingServiceClient") RestClient client) {
        this.client = client;
    }

    @Override
    public boolean exists(UUID buildingId) {
        try {
            client.get().uri("/api/v1/platform/buildings/{id}", buildingId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound unknownBuilding) {
            return false;
        } catch (RestClientException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
    }
}
