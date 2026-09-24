package com.buildingos.building.ownership.presentation.rest.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AssignOwnershipRequest(UUID ownerUserId, BigDecimal share, LocalDate effectiveDate, String notes,
        String reason, Long expectedVersion, UUID operationId) {}
