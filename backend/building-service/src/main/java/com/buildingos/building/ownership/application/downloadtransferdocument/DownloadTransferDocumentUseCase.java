package com.buildingos.building.ownership.application.downloadtransferdocument;

import com.buildingos.building.shared.application.Actor;

public interface DownloadTransferDocumentUseCase {
    TransferDocumentContent execute(Actor actor, DownloadTransferDocumentQuery query);
}
