package com.buildingos.building.building.presentation.rest;

import com.buildingos.building.building.application.listplatformbuildings.ListPlatformBuildingsQuery;
import com.buildingos.building.building.application.listplatformbuildings.ListPlatformBuildingsUseCase;
import com.buildingos.building.building.domain.model.BuildingStatus;
import com.buildingos.building.building.presentation.rest.response.BuildingSummaryResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.building.shared.presentation.rest.Enums;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Platform building list for SUPER_ADMIN / PLATFORM_ADMIN (D-34). */
@RestController
@RequestMapping("/api/v1/platform/buildings")
public class PlatformBuildingListController {
    private final ListPlatformBuildingsUseCase list;

    public PlatformBuildingListController(ListPlatformBuildingsUseCase list) { this.list = list; }

    @GetMapping
    public ApiEnvelope<List<BuildingSummaryResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = list.execute(CurrentActor.from(jwt),
                new ListPlatformBuildingsQuery(Enums.parseOptional(BuildingStatus.class, status, "status"), q, page,
                        size));
        return new ApiEnvelope<>(true, result.items().stream().map(BuildingSummaryResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }
}
