package com.buildingos.building.duplicate.domain.model;

import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import java.util.UUID;

/** Another application or an existing building that might be the same place. {@code reference}: number or name. */
public record DuplicateCandidate(CandidateKind kind, UUID id, String reference, String status, String name,
        String address, String area, String district, String contactPhone, Coordinates coordinates) {}
