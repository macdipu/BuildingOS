package com.buildingos.backoffice.supportsession.presentation.rest.response;

import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code permissionScope} lists every named scope; high-risk ones are usable only once approved. */
public record SupportSessionResponse(UUID id, UUID platformUserId, UUID targetUserId, UUID buildingId, String status,
        String reason, List<String> permissionScope, Instant startedAt, Instant expiresAt, Instant endedAt) {
    public static SupportSessionResponse of(SupportSession s) {
        return new SupportSessionResponse(s.id(), s.platformUserId(), s.targetUserId(), s.buildingId(),
                s.status().name(), s.reason(), s.permissionScope().stream().map(Enum::name).toList(), s.startedAt(),
                s.expiresAt(), s.endedAt());
    }
}
