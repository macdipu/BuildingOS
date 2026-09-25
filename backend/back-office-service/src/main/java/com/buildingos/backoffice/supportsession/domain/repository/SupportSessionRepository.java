package com.buildingos.backoffice.supportsession.domain.repository;

import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionFilter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportSessionRepository {
    void insert(SupportSession session);
    /** Persists {@code ended_at} and {@code permission_scope}. */
    void update(SupportSession session);
    Optional<SupportSession> findById(UUID id);
    /** Locks the row until the surrounding transaction ends. */
    Optional<SupportSession> findByIdForUpdate(UUID id);
    /** Not-ended sessions at or past {@code expires_at}, locked; rows locked by another transaction are skipped. */
    List<SupportSession> lockDueForExpiry(Instant now, int limit);
    /** Newest {@code started_at} first. */
    List<SupportSession> search(SupportSessionFilter filter, int offset, int limit);
    long count(SupportSessionFilter filter);
}
