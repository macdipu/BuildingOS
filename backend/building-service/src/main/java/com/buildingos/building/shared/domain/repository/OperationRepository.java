package com.buildingos.building.shared.domain.repository;

import com.buildingos.building.shared.domain.model.RecordedOperation;
import java.util.Optional;
import java.util.UUID;

public interface OperationRepository {
    Optional<RecordedOperation> find(UUID actorUserId, String action, UUID operationId);
    void insert(RecordedOperation operation);
}
