package com.buildingos.building.membership.presentation.rest;

import com.buildingos.building.membership.application.getbuildingcontext.GetBuildingContextQuery;
import com.buildingos.building.membership.application.getbuildingcontext.GetBuildingContextUseCase;
import com.buildingos.building.membership.application.listmybuildings.ListMyBuildingsQuery;
import com.buildingos.building.membership.application.listmybuildings.ListMyBuildingsUseCase;
import com.buildingos.building.membership.presentation.rest.response.BuildingContextResponse;
import com.buildingos.building.membership.presentation.rest.response.MyBuildingResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Building selector and context (UO-01). */
@RestController
public class BuildingPortfolioController {
    private final ListMyBuildingsUseCase listMine;
    private final GetBuildingContextUseCase context;

    public BuildingPortfolioController(ListMyBuildingsUseCase listMine, GetBuildingContextUseCase context) {
        this.listMine = listMine;
        this.context = context;
    }

    @GetMapping("/api/v1/me/buildings")
    public ApiEnvelope<List<MyBuildingResponse>> myBuildings(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = listMine.execute(CurrentActor.from(jwt), new ListMyBuildingsQuery(page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(MyBuildingResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/api/v1/buildings/{buildingId}")
    public ApiEnvelope<BuildingContextResponse> building(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, HttpServletRequest request) {
        var view = context.execute(CurrentActor.from(jwt), new GetBuildingContextQuery(buildingId));
        return ApiEnvelope.of(BuildingContextResponse.of(view), CorrelationFilter.traceId(request));
    }
}
