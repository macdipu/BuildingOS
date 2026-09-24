package com.buildingos.building.outbox.application.publishpendingevents;

/** {@code deferred}: events held back because an earlier event of the same aggregate failed in this pass. */
public record PublishPendingEventsResult(int published, int failed, int deferred) {
}
