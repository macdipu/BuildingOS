package com.buildingos.backoffice.supportsession.domain.model;

import java.util.UUID;

/** Optional list filters; a null field does not filter. {@code active} true = not ended, false = ended. */
public record SupportSessionFilter(Boolean active, UUID platformUserId, UUID targetUserId, UUID buildingId) {}
