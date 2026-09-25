package com.buildingos.backoffice.supportsession.application.startsupportsession;

import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartSupportSessionCommand(UUID targetUserId, UUID buildingId, List<SupportScope> permissionScope,
        String reason, Instant expiresAt) {}
