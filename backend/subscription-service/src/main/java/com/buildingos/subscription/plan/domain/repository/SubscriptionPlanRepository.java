package com.buildingos.subscription.plan.domain.repository;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository {
    /** Inserts; returns false if the code is already taken. */
    boolean insert(SubscriptionPlan plan);
    void update(SubscriptionPlan plan);
    Optional<SubscriptionPlan> findById(UUID id);
    /** Row-locks the plan for the surrounding unit of work. */
    Optional<SubscriptionPlan> findByIdForUpdate(UUID id);
    List<SubscriptionPlan> findAll();
}
