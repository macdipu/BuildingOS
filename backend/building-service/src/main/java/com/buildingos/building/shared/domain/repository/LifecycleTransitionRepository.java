package com.buildingos.building.shared.domain.repository;

import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LifecycleTransitionRepository {
    void append(LifecycleTransition transition);
    /** Oldest first. */
    List<LifecycleTransition> findFor(EntityType type, UUID entityId);

    /**
     * Transitions with {@code since <= occurredAt < until}, of {@code entityType}, by {@code actorUserId} (each filter
     * ignored if null), newest first ({@code occurredAt}, then id), at most {@code limit} (F6-T5c).
     */
    List<LifecycleTransition> list(Instant since, Instant until, String entityType, UUID actorUserId, int limit);
}
