package com.buildingos.backoffice.shared.application.port.out;

import java.util.UUID;

/** building-service buildings. Throws {@code DependencyUnavailableException} when building-service cannot answer. */
public interface BuildingDirectory {
    boolean exists(UUID buildingId);
}
