package com.buildingos.backoffice.supportsession.presentation.rest.request;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartSupportSessionRequest(UUID targetUserId, UUID buildingId, List<String> permissionScope,
        String reason, Instant expiresAt) {}
