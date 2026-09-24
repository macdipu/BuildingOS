package com.buildingos.building.membership.presentation.rest.response;

import com.buildingos.building.building.domain.model.BuildingMembership;
import java.time.Instant;
import java.util.UUID;

public record MemberResponse(UUID id, UUID buildingId, UUID userId, String role, String status, long version,
        Instant createdAt, Instant updatedAt, Instant revokedAt) {
    public static MemberResponse of(BuildingMembership m) {
        return new MemberResponse(m.id(), m.buildingId(), m.userId(), m.role().name(), m.status().name(), m.version(),
                m.createdAt(), m.updatedAt(), m.revokedAt());
    }
}
