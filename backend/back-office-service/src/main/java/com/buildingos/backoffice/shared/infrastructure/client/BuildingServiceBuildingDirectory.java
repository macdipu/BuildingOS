package com.buildingos.backoffice.shared.infrastructure.client;

import com.buildingos.backoffice.shared.application.port.out.BuildingDirectory;
import com.buildingos.backoffice.shared.application.DependencyUnavailableException;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** building-service {@code GET /internal/directory/buildings/{id}} with this service's own token (D-38). */
@Component
public class BuildingServiceBuildingDirectory implements BuildingDirectory {
    private static final String DEPENDENCY = "building-service";
    private final RestClient client;

    public BuildingServiceBuildingDirectory(@Qualifier("buildingDirectoryClient") RestClient client) {
        this.client = client;
    }

    @Override
    public boolean exists(UUID buildingId) {
        try {
            client.get().uri("/internal/directory/buildings/{id}", buildingId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound unknownBuilding) {
            return false;
        } catch (HttpClientErrorException.Forbidden denied) {
            // D-38: a refused lookup is a 403, not an outage.
            throw new NotPermittedException();
        } catch (RestClientException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
    }
}
