package com.buildingos.building.note.application.listnotes;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.note.domain.model.InternalNote;
import com.buildingos.building.note.domain.repository.InternalNoteRepository;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public final class ListNotesService implements ListNotesUseCase {
    private final ApplicationChanges applications;
    private final InternalNoteRepository notes;

    public ListNotesService(ApplicationChanges applications, InternalNoteRepository notes) {
        this.applications = applications;
        this.notes = notes;
    }

    @Override
    public List<InternalNote> execute(Actor actor, ListNotesQuery query) {
        var application = applications.load(actor, query.applicationId(), ApplicationAccess.PLATFORM_ADMIN);
        return notes.findByApplication(application.id());
    }
}
