package com.buildingos.backoffice.systemhealth.domain.model;

import java.util.Collection;

/** BOC-08 roll-up: UP when every service is UP, DOWN when none is, otherwise DEGRADED. */
public enum OverallHealth {
    UP, DEGRADED, DOWN;

    public static OverallHealth of(Collection<ServiceHealthStatus> statuses) {
        long up = statuses.stream().filter(ServiceHealthStatus.UP::equals).count();
        if (!statuses.isEmpty() && up == statuses.size()) {
            return UP;
        }
        return up == 0 ? DOWN : DEGRADED;
    }
}
