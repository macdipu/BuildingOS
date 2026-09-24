package com.buildingos.building.ownership.application.uploadtransferdocument;

import com.buildingos.building.ownership.domain.model.TransferDocument;
import com.buildingos.building.shared.application.Actor;

public interface UploadTransferDocumentUseCase {
    TransferDocument execute(Actor actor, UploadTransferDocumentCommand command);
}
