package com.buildingos.building.ownership.application.assignownership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** {@code expectedVersion} is the unit's ownership revision. */
public record AssignOwnershipCommand(UUID buildingId, UUID unitId, UUID ownerUserId, BigDecimal share,
        LocalDate effectiveDate, String notes, String reason, Long expectedVersion, UUID operationId) {}
