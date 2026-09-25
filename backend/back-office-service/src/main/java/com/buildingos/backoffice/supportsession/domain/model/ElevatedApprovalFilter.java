package com.buildingos.backoffice.supportsession.domain.model;

import java.util.UUID;

/** Optional list filters; a null field does not filter. {@code sessionOwnerUserId} restricts to one owner's sessions. */
public record ElevatedApprovalFilter(ElevatedApprovalStatus status, UUID sessionOwnerUserId) {}
