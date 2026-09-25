package com.buildingos.backoffice.systemhealth.presentation.rest.response;

import com.buildingos.backoffice.systemhealth.domain.model.ServiceReadiness;
import com.buildingos.backoffice.systemhealth.domain.model.SystemHealth;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** Only status, HTTP code and latency per service; readiness bodies are never forwarded. */
public record SystemHealthResponse(Instant checkedAt, String overall, List<Service> services) {
    /** {@code httpStatus} is omitted when the service was unreachable or timed out. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Service(String name, String status, Integer httpStatus, long latencyMs) {
        static Service of(ServiceReadiness readiness) {
            return new Service(readiness.name(), readiness.status().name(), readiness.httpStatus(),
                    readiness.latencyMs());
        }
    }

    public static SystemHealthResponse of(SystemHealth health) {
        return new SystemHealthResponse(health.checkedAt(), health.overall().name(),
                health.services().stream().map(Service::of).toList());
    }
}
