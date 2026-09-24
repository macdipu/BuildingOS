package com.buildingos.building.document.application.uploaddocument;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.shared.application.Actor;

public interface UploadDocumentUseCase {
    ApplicationDocument execute(Actor actor, UploadDocumentCommand command);
}
