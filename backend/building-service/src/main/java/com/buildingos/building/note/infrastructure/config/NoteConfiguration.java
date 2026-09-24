package com.buildingos.building.note.infrastructure.config;

import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.note.application.addnote.AddNoteService;
import com.buildingos.building.note.application.addnote.AddNoteUseCase;
import com.buildingos.building.note.application.listnotes.ListNotesService;
import com.buildingos.building.note.application.listnotes.ListNotesUseCase;
import com.buildingos.building.note.domain.repository.InternalNoteRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NoteConfiguration {
    @Bean
    AddNoteUseCase addNote(ApplicationChanges applications, InternalNoteRepository notes, Clock clock) {
        return new AddNoteService(applications, notes, clock);
    }

    @Bean
    ListNotesUseCase listNotes(ApplicationChanges applications, InternalNoteRepository notes) {
        return new ListNotesService(applications, notes);
    }
}
