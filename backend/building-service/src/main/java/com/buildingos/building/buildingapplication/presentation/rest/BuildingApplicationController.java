package com.buildingos.building.buildingapplication.presentation.rest;

import com.buildingos.building.buildingapplication.application.createapplication.CreateApplicationCommand;
import com.buildingos.building.buildingapplication.application.createapplication.CreateApplicationUseCase;
import com.buildingos.building.buildingapplication.application.getapplication.GetApplicationQuery;
import com.buildingos.building.buildingapplication.application.getapplication.GetApplicationUseCase;
import com.buildingos.building.buildingapplication.application.getapplicationhistory.GetApplicationHistoryQuery;
import com.buildingos.building.buildingapplication.application.getapplicationhistory.GetApplicationHistoryUseCase;
import com.buildingos.building.buildingapplication.application.listmyapplications.ListMyApplicationsUseCase;
import com.buildingos.building.buildingapplication.application.submitapplication.SubmitApplicationCommand;
import com.buildingos.building.buildingapplication.application.submitapplication.SubmitApplicationUseCase;
import com.buildingos.building.buildingapplication.application.updateapplication.UpdateApplicationCommand;
import com.buildingos.building.buildingapplication.application.updateapplication.UpdateApplicationUseCase;
import com.buildingos.building.buildingapplication.presentation.rest.mapper.ApplicationRequestMapper;
import com.buildingos.building.buildingapplication.presentation.rest.request.ApplicationRequest;
import com.buildingos.building.buildingapplication.presentation.rest.response.ApplicationResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.building.shared.presentation.rest.TransitionResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Applicant-facing endpoints (any authenticated user; only their own applications). */
@RestController
@RequestMapping("/api/v1")
public class BuildingApplicationController {
    private final CreateApplicationUseCase create;
    private final UpdateApplicationUseCase update;
    private final SubmitApplicationUseCase submit;
    private final GetApplicationUseCase get;
    private final ListMyApplicationsUseCase listMine;
    private final GetApplicationHistoryUseCase history;

    public BuildingApplicationController(CreateApplicationUseCase create, UpdateApplicationUseCase update,
            SubmitApplicationUseCase submit, GetApplicationUseCase get, ListMyApplicationsUseCase listMine,
            GetApplicationHistoryUseCase history) {
        this.create = create;
        this.update = update;
        this.submit = submit;
        this.get = get;
        this.listMine = listMine;
        this.history = history;
    }

    @PostMapping("/building-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<ApplicationResponse> create(@AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) ApplicationRequest body, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        var created = create.execute(actor, new CreateApplicationCommand(ApplicationRequestMapper.toDetails(body)));
        return ApiEnvelope.of(ApplicationResponse.of(created, false), CorrelationFilter.traceId(request));
    }

    @GetMapping("/me/building-applications")
    public ApiEnvelope<List<ApplicationResponse>> listMine(@AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiEnvelope.of(listMine.execute(CurrentActor.from(jwt)).stream()
                .map(a -> ApplicationResponse.of(a, false)).toList(), CorrelationFilter.traceId(request));
    }

    @GetMapping("/building-applications/{applicationId}")
    public ApiEnvelope<ApplicationResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        var found = get.execute(actor, new GetApplicationQuery(applicationId));
        return ApiEnvelope.of(ApplicationResponse.of(found, actor.isPlatformAdmin()), CorrelationFilter.traceId(request));
    }

    @PutMapping("/building-applications/{applicationId}")
    public ApiEnvelope<ApplicationResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            @RequestBody ApplicationRequest body, HttpServletRequest request) {
        var updated = update.execute(CurrentActor.from(jwt),
                new UpdateApplicationCommand(applicationId, ApplicationRequestMapper.toDetails(body)));
        return ApiEnvelope.of(ApplicationResponse.of(updated, false), CorrelationFilter.traceId(request));
    }

    @PostMapping("/building-applications/{applicationId}/submit")
    public ApiEnvelope<ApplicationResponse> submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            HttpServletRequest request) {
        var submitted = submit.execute(CurrentActor.from(jwt), new SubmitApplicationCommand(applicationId));
        return ApiEnvelope.of(ApplicationResponse.of(submitted, false), CorrelationFilter.traceId(request));
    }

    @GetMapping("/building-applications/{applicationId}/history")
    public ApiEnvelope<List<TransitionResponse>> history(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, HttpServletRequest request) {
        return ApiEnvelope.of(history.execute(CurrentActor.from(jwt), new GetApplicationHistoryQuery(applicationId))
                .stream().map(TransitionResponse::of).toList(), CorrelationFilter.traceId(request));
    }
}
