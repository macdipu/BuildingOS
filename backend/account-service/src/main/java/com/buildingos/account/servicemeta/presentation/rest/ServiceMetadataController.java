package com.buildingos.account.servicemeta.presentation.rest;

import com.buildingos.account.servicemeta.application.getservicemetadata.GetServiceMetadataQuery;
import com.buildingos.account.servicemeta.application.getservicemetadata.GetServiceMetadataUseCase;
import com.buildingos.account.servicemeta.presentation.rest.response.ServiceMetadataResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceMetadataController {
    private final GetServiceMetadataUseCase query;
    public ServiceMetadataController(GetServiceMetadataUseCase query) { this.query = query; }

    @GetMapping("/internal/platform/info")
    public ApiEnvelope<ServiceMetadataResponse> info(HttpServletRequest request) {
        return ApiEnvelope.of(new ServiceMetadataResponse(query.execute(new GetServiceMetadataQuery()).service()),
                CorrelationFilter.traceId(request));
    }
}
