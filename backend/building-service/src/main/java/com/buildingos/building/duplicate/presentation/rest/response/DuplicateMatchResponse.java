package com.buildingos.building.duplicate.presentation.rest.response;

import com.buildingos.building.duplicate.domain.model.DuplicateMatch;
import java.util.List;
import java.util.UUID;

public record DuplicateMatchResponse(String kind, UUID id, String reference, String status, String name,
        String address, String area, String district, List<String> matchedOn) {
    public static DuplicateMatchResponse of(DuplicateMatch match) {
        var c = match.candidate();
        return new DuplicateMatchResponse(c.kind().name(), c.id(), c.reference(), c.status(), c.name(), c.address(),
                c.area(), c.district(), match.signals().stream().map(Enum::name).sorted().toList());
    }
}
