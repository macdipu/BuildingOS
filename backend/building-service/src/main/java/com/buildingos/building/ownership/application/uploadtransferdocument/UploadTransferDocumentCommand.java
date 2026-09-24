package com.buildingos.building.ownership.application.uploadtransferdocument;

import java.util.UUID;

/** {@code reason} is optional for building admins and required for platform administrators. */
public record UploadTransferDocumentCommand(UUID buildingId, UUID unitId, UUID transferId, String fileName,
        byte[] content, String reason) {}
