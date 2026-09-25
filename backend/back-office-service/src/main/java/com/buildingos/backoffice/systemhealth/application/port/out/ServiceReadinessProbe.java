package com.buildingos.backoffice.systemhealth.application.port.out;

import com.buildingos.backoffice.systemhealth.domain.model.ServiceReadiness;
import java.util.List;

/** Reads a BuildingOS service's existing readiness endpoint. Never throws: failures map to UNKNOWN. */
public interface ServiceReadinessProbe {
    /** Configured services to check, in display order. */
    List<String> monitoredServices();

    ServiceReadiness probe(String serviceName);
}
