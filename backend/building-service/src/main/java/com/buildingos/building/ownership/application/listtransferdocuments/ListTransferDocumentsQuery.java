package com.buildingos.building.ownership.application.listtransferdocuments;

import java.util.UUID;

public record ListTransferDocumentsQuery(UUID buildingId, UUID unitId, UUID transferId) {}
