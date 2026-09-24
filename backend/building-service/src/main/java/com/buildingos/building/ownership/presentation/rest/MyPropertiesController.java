package com.buildingos.building.ownership.presentation.rest;

import com.buildingos.building.ownership.application.listmyproperties.ListMyPropertiesQuery;
import com.buildingos.building.ownership.application.listmyproperties.ListMyPropertiesUseCase;
import com.buildingos.building.ownership.presentation.rest.response.PropertyResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The caller's current unit allocations across buildings (UO-08). */
@RestController
public class MyPropertiesController {
    private final ListMyPropertiesUseCase list;

    public MyPropertiesController(ListMyPropertiesUseCase list) {
        this.list = list;
    }

    @GetMapping("/api/v1/me/properties")
    public ApiEnvelope<List<PropertyResponse>> properties(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = list.execute(CurrentActor.from(jwt), new ListMyPropertiesQuery(page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(PropertyResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }
}
