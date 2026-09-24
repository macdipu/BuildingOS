package com.buildingos.building.note.presentation.rest.response;

import com.buildingos.building.note.domain.model.InternalNote;
import java.time.Instant;
import java.util.UUID;

public record NoteResponse(UUID id, UUID authorUserId, String body, Instant createdAt) {
    public static NoteResponse of(InternalNote note) {
        return new NoteResponse(note.id(), note.authorUserId(), note.body(), note.createdAt());
    }
}
