package com.buildingos.building.duplicate.domain.repository;

import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import com.buildingos.building.duplicate.domain.model.DuplicateCandidate;
import java.util.List;
import java.util.UUID;

public interface DuplicateCandidateRepository {
    /**
     * Existing buildings and open applications (not draft, rejected or already approved) other than
     * {@code excludeApplicationId}, narrowed to the same district, the same contact phone, or roughly within
     * {@code radiusMeters} of {@code near}. Any argument may be null.
     */
    List<DuplicateCandidate> findCandidates(UUID excludeApplicationId, String district, String contactPhone,
            Coordinates near, double radiusMeters);
}
