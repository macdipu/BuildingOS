package com.buildingos.building.note.application.listnotes;

import com.buildingos.building.note.domain.model.InternalNote;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public interface ListNotesUseCase {
    List<InternalNote> execute(Actor actor, ListNotesQuery query);
}
