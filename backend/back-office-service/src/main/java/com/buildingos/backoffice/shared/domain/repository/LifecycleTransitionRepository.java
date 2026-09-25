package com.buildingos.backoffice.shared.domain.repository;

import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LifecycleTransitionRepository {
    void append(LifecycleTransition transition);
    /** Oldest first. */
    List<LifecycleTransition> findFor(EntityType type, UUID entityId);
    /**
     * Audit read (F6-T5d): newest first (occurredAt, then id), at most {@code limit}. Every filter is optional (null);
     * {@code since} inclusive, {@code until} exclusive.
     */
    List<LifecycleTransition> list(Instant since, Instant until, EntityType type, UUID actorUserId, int limit);
}
