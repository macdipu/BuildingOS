package com.buildingos.building.ownership.application.getcurrentownerships;

import java.util.UUID;

public record GetCurrentOwnershipsQuery(UUID buildingId, UUID unitId) {}
