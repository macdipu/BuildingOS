package com.buildingos.building.document.application;

import com.buildingos.building.shared.application.BusinessRuleException;
import java.util.UUID;

public final class DocumentErrors {
    private DocumentErrors() {}

    public static BusinessRuleException notFound(UUID id) {
        return BusinessRuleException.notFound("DOCUMENT_NOT_FOUND", "Document " + id + " does not exist");
    }
}
