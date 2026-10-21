package com.buildingos.building.building.presentation.rest;

import com.buildingos.building.building.application.getdirectorybuilding.GetDirectoryBuildingQuery;
import com.buildingos.building.building.application.getdirectorybuilding.GetDirectoryBuildingUseCase;
import com.buildingos.building.building.presentation.rest.response.DirectoryBuildingResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.platform.web.security.ServicePrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only (D-38): not routed by the gateway; requires a service token with {@code directory.read}. */
@RestController
public class DirectoryBuildingController {
    private final GetDirectoryBuildingUseCase get;

    public DirectoryBuildingController(GetDirectoryBuildingUseCase get) {
        this.get = get;
    }

    @GetMapping("/internal/directory/buildings/{buildingId}")
    public ApiEnvelope<DirectoryBuildingResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            HttpServletRequest request) {
        var building = get.execute(new GetDirectoryBuildingQuery(ServicePrincipal.scopes(jwt), buildingId));
        return ApiEnvelope.of(DirectoryBuildingResponse.of(building), CorrelationFilter.traceId(request));
    }
}
