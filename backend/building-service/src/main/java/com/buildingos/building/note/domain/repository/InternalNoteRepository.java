package com.buildingos.building.note.domain.repository;

import com.buildingos.building.note.domain.model.InternalNote;
import java.util.List;
import java.util.UUID;

public interface InternalNoteRepository {
    void insert(InternalNote note);
    /** Oldest first. */
    List<InternalNote> findByApplication(UUID applicationId);
}
