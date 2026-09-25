package com.buildingos.backoffice.shared.domain.repository;

import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import java.util.List;
import java.util.UUID;

public interface LifecycleTransitionRepository {
    void append(LifecycleTransition transition);
    /** Oldest first. */
    List<LifecycleTransition> findFor(EntityType type, UUID entityId);
}
