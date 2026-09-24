package com.buildingos.building.ownership.application.transferownership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** {@code expectedVersion} is the unit's ownership revision; {@code reference} is optional. */
public record TransferOwnershipCommand(UUID buildingId, UUID unitId, UUID sourceOwnerUserId, UUID recipientUserId,
        BigDecimal share, LocalDate effectiveDate, String reference, String reason, Long expectedVersion,
        UUID operationId) {}
