package com.buildingos.backoffice.supportsession.domain.repository;

import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalFilter;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElevatedApprovalRequestRepository {
    void insert(ElevatedApprovalRequest request);
    /** Persists the decision (status, decided by/at, reason). */
    void update(ElevatedApprovalRequest request);
    Optional<ElevatedApprovalRequest> findById(UUID id);
    /** Locks the row until the surrounding transaction ends. */
    Optional<ElevatedApprovalRequest> findByIdForUpdate(UUID id);
    /** Oldest first. */
    List<ElevatedApprovalRequest> findBySession(UUID supportSessionId);
    /** Newest {@code requested_at} first. */
    List<ElevatedApprovalRequest> search(ElevatedApprovalFilter filter, int offset, int limit);
    long count(ElevatedApprovalFilter filter);
}
