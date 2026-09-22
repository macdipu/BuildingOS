package com.buildingos.building.platform.presentation.rest;

import com.buildingos.building.platform.application.port.in.GetServiceMetadata;
import com.buildingos.platform.web.ApiEnvelope;
import com.buildingos.platform.web.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlatformController {
    private final GetServiceMetadata query;
    public PlatformController(GetServiceMetadata query) { this.query = query; }

    @GetMapping("/internal/platform/info")
    public ApiEnvelope<MetadataResponse> info(HttpServletRequest request) {
        return ApiEnvelope.of(new MetadataResponse(query.execute().service()), CorrelationFilter.traceId(request));
    }
    public record MetadataResponse(String service) {}
}
