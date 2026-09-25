package com.buildingos.backoffice.supportsession.application.listsupportsessions;

import java.util.UUID;

/** Optional filters; a null filter matches everything. */
public record ListSupportSessionsQuery(Boolean active, UUID platformUserId, UUID targetUserId, UUID buildingId,
        int page, int size) {}
