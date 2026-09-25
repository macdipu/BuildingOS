package com.buildingos.backoffice.onboarding.domain.repository;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionFilter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssistedOnboardingSessionRepository {
    void insert(AssistedOnboardingSession session);
    void update(AssistedOnboardingSession session);
    Optional<AssistedOnboardingSession> findById(UUID id);
    /** Locks the row until the surrounding transaction ends. */
    Optional<AssistedOnboardingSession> findByIdForUpdate(UUID id);
    /** Active sessions at or past {@code expires_at}, locked; rows locked by another transaction are skipped. */
    List<AssistedOnboardingSession> lockDueForExpiry(Instant now, int limit);
    /** Newest {@code started_at} first. */
    List<AssistedOnboardingSession> search(OnboardingSessionFilter filter, int offset, int limit);
    long count(OnboardingSessionFilter filter);
}
