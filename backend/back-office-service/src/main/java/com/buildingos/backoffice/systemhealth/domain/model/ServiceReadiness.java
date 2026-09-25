package com.buildingos.backoffice.systemhealth.domain.model;

import java.util.Objects;

/** Readiness probe result: status, the HTTP code when the service answered (else null) and round-trip latency. */
public record ServiceReadiness(String name, ServiceHealthStatus status, Integer httpStatus, long latencyMs) {
    public ServiceReadiness {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        latencyMs = Math.max(0, latencyMs);
    }

    public static ServiceReadiness unknown(String name, long latencyMs) {
        return new ServiceReadiness(name, ServiceHealthStatus.UNKNOWN, null, latencyMs);
    }
}
