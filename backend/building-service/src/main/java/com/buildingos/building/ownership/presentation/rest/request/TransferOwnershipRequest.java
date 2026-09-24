package com.buildingos.building.ownership.presentation.rest.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransferOwnershipRequest(UUID sourceOwnerUserId, UUID recipientUserId, BigDecimal share,
        LocalDate effectiveDate, String reference, String reason, Long expectedVersion, UUID operationId) {}
