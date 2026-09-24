package com.buildingos.building.document.application.uploaddocument;

import java.util.UUID;

public record UploadDocumentCommand(UUID applicationId, String fileName, byte[] content) {}
