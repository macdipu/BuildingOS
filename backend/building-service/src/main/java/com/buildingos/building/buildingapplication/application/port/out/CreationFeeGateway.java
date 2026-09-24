package com.buildingos.building.buildingapplication.application.port.out;

import java.util.UUID;

/**
 * Building-creation fee status from subscription-service (D-24, D-26). A fee that is not configured is a
 * {@code FEE_NOT_CONFIGURED} conflict (fail closed); an unreachable service is a dependency failure.
 */
public interface CreationFeeGateway {
    CreationFeeStatus status(UUID applicationId);
}
