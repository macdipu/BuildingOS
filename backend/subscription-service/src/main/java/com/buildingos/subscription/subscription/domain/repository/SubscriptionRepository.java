package com.buildingos.subscription.subscription.domain.repository;

import com.buildingos.subscription.subscription.domain.model.Subscription;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;
import java.util.Optional;

public interface SubscriptionRepository {
    Optional<Subscription> findActive(SubscriptionSubject subject);
    /** Inserts unless the subject already has an active subscription; returns false in that case. */
    boolean insertIfNoneActive(Subscription subscription);
}
