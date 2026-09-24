package com.buildingos.building.unit.presentation.rest.response;

import com.buildingos.building.unit.domain.model.Floor;
import java.time.Instant;
import java.util.UUID;

public record FloorResponse(UUID id, UUID buildingId, String label, String kind, int displayOrder, long version,
        Instant createdAt, Instant updatedAt) {
    public static FloorResponse of(Floor f) {
        var d = f.details();
        return new FloorResponse(f.id(), f.buildingId(), d.label(), d.kind().name(), d.displayOrder(), f.version(),
                f.createdAt(), f.updatedAt());
    }
}
