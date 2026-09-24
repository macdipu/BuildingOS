package com.buildingos.building.document.application.removedocument;

import java.util.UUID;

public record RemoveDocumentCommand(UUID applicationId, UUID documentId) {}
