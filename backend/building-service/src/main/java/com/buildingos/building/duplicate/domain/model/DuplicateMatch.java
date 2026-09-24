package com.buildingos.building.duplicate.domain.model;

import java.util.Set;

public record DuplicateMatch(DuplicateCandidate candidate, Set<DuplicateSignal> signals) {
    public DuplicateMatch {
        signals = Set.copyOf(signals);
    }
}
