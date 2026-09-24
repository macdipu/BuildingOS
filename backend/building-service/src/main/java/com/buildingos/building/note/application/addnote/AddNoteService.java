package com.buildingos.building.note.application.addnote;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.note.domain.model.InternalNote;
import com.buildingos.building.note.domain.repository.InternalNoteRepository;
import com.buildingos.building.shared.application.Actor;
import java.time.Clock;
import java.util.UUID;

public final class AddNoteService implements AddNoteUseCase {
    private final ApplicationChanges applications;
    private final InternalNoteRepository notes;
    private final Clock clock;

    public AddNoteService(ApplicationChanges applications, InternalNoteRepository notes, Clock clock) {
        this.applications = applications;
        this.notes = notes;
        this.clock = clock;
    }

    @Override
    public InternalNote execute(Actor actor, AddNoteCommand command) {
        var application = applications.load(actor, command.applicationId(), ApplicationAccess.PLATFORM_ADMIN);
        var note = new InternalNote(UUID.randomUUID(), application.id(), actor.userId(), command.body(), clock.instant());
        notes.insert(note);
        return note;
    }
}
