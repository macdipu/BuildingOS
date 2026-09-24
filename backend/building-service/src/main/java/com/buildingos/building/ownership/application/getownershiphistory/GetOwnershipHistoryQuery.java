package com.buildingos.building.ownership.application.getownershiphistory;

import java.util.UUID;

public record GetOwnershipHistoryQuery(UUID buildingId, UUID unitId) {}
