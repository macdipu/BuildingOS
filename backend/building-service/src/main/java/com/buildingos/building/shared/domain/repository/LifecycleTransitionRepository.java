package com.buildingos.building.shared.domain.repository;

import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import java.util.List;
import java.util.UUID;

public interface LifecycleTransitionRepository {
    void append(LifecycleTransition transition);
    /** Oldest first. */
    List<LifecycleTransition> findFor(EntityType type, UUID entityId);
}
