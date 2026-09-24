package com.buildingos.building.ownership.application.removetransferdocument;

import com.buildingos.building.shared.application.Actor;

public interface RemoveTransferDocumentUseCase {
    void execute(Actor actor, RemoveTransferDocumentCommand command);
}
