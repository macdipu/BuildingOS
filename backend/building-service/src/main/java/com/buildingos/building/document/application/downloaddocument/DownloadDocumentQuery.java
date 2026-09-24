package com.buildingos.building.document.application.downloaddocument;

import java.util.UUID;

public record DownloadDocumentQuery(UUID applicationId, UUID documentId) {}
