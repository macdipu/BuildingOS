package com.buildingos.building.ownership.application.removetransferdocument;

import java.util.UUID;

public record RemoveTransferDocumentCommand(UUID buildingId, UUID unitId, UUID transferId, UUID documentId,
        String reason) {}
