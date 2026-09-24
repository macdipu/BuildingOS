package com.buildingos.building.note.application.addnote;

import com.buildingos.building.note.domain.model.InternalNote;
import com.buildingos.building.shared.application.Actor;

public interface AddNoteUseCase {
    InternalNote execute(Actor actor, AddNoteCommand command);
}
