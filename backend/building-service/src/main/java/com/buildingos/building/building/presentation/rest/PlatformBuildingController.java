package com.buildingos.building.building.presentation.rest;

import com.buildingos.building.building.application.BuildingLifecycleCommand;
import com.buildingos.building.building.application.activatebuilding.ActivateBuildingUseCase;
import com.buildingos.building.building.application.getbuilding.GetBuildingQuery;
import com.buildingos.building.building.application.getbuilding.GetBuildingUseCase;
import com.buildingos.building.building.application.reactivatebuilding.ReactivateBuildingUseCase;
import com.buildingos.building.building.application.suspendbuilding.SuspendBuildingUseCase;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.presentation.rest.request.LifecycleReasonRequest;
import com.buildingos.building.building.presentation.rest.response.BuildingResponse;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Building lifecycle for SUPER_ADMIN / PLATFORM_ADMIN (AP-09). */
@RestController
@RequestMapping("/api/v1/platform/buildings/{buildingId}")
public class PlatformBuildingController {
    private final GetBuildingUseCase get;
    private final ActivateBuildingUseCase activate;
    private final SuspendBuildingUseCase suspend;
    private final ReactivateBuildingUseCase reactivate;

    public PlatformBuildingController(GetBuildingUseCase get, ActivateBuildingUseCase activate,
            SuspendBuildingUseCase suspend, ReactivateBuildingUseCase reactivate) {
        this.get = get;
        this.activate = activate;
        this.suspend = suspend;
        this.reactivate = reactivate;
    }

    @GetMapping
    public ApiEnvelope<BuildingResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            HttpServletRequest request) {
        return ApiEnvelope.of(view(CurrentActor.from(jwt), buildingId), CorrelationFilter.traceId(request));
    }

    @PostMapping("/activate")
    public ApiEnvelope<BuildingResponse> activate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestBody LifecycleReasonRequest body, HttpServletRequest request) {
        return act(jwt, buildingId, body, activate::execute, request);
    }

    @PostMapping("/suspend")
    public ApiEnvelope<BuildingResponse> suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestBody LifecycleReasonRequest body, HttpServletRequest request) {
        return act(jwt, buildingId, body, suspend::execute, request);
    }

    @PostMapping("/reactivate")
    public ApiEnvelope<BuildingResponse> reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestBody LifecycleReasonRequest body, HttpServletRequest request) {
        return act(jwt, buildingId, body, reactivate::execute, request);
    }

    private ApiEnvelope<BuildingResponse> act(Jwt jwt, UUID buildingId, LifecycleReasonRequest body,
            BiFunction<Actor, BuildingLifecycleCommand, Building> action, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        action.apply(actor, new BuildingLifecycleCommand(buildingId, body.reason()));
        return ApiEnvelope.of(view(actor, buildingId), CorrelationFilter.traceId(request));
    }

    private BuildingResponse view(Actor actor, UUID buildingId) {
        var view = get.execute(actor, new GetBuildingQuery(buildingId));
        return BuildingResponse.of(view.building(), view.memberships());
    }
}
