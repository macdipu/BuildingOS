package com.buildingos.building.note.application.addnote;

import java.util.UUID;

public record AddNoteCommand(UUID applicationId, String body) {}
