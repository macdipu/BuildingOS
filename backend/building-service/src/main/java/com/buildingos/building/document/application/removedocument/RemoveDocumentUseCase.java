package com.buildingos.building.document.application.removedocument;

import com.buildingos.building.shared.application.Actor;

public interface RemoveDocumentUseCase {
    void execute(Actor actor, RemoveDocumentCommand command);
}
