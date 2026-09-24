package com.buildingos.building.shared.presentation.rest;

import com.buildingos.building.shared.domain.model.LifecycleTransition;
import java.time.Instant;

public record TransitionResponse(String fromStatus, String toStatus, String reason, Instant occurredAt) {
    public static TransitionResponse of(LifecycleTransition t) {
        return new TransitionResponse(t.fromStatus(), t.toStatus(), t.reason(), t.occurredAt());
    }
}
