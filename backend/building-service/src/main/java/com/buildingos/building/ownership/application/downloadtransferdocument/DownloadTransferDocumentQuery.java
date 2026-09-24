package com.buildingos.building.ownership.application.downloadtransferdocument;

import java.util.UUID;

public record DownloadTransferDocumentQuery(UUID buildingId, UUID unitId, UUID transferId, UUID documentId) {}
