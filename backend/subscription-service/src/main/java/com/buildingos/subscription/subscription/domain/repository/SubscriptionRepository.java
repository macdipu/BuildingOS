package com.buildingos.subscription.subscription.domain.repository;

import com.buildingos.subscription.subscription.domain.model.PlatformSubscriptionRow;
import com.buildingos.subscription.subscription.domain.model.Subscription;
import com.buildingos.subscription.subscription.domain.model.SubscriptionStatus;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Optional<Subscription> findActive(SubscriptionSubject subject);
    /** Inserts unless the subject already has an active subscription; returns false in that case. */
    boolean insertIfNoneActive(Subscription subscription);
    /** Newest first; null status = all, null planCode = all plans. */
    List<PlatformSubscriptionRow> findPage(SubscriptionStatus status, String planCode, int offset, int limit);
    long count(SubscriptionStatus status, String planCode);
}
