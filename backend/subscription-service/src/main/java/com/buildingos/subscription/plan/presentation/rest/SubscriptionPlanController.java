package com.buildingos.subscription.plan.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.plan.application.createplan.CreatePlanUseCase;
import com.buildingos.subscription.plan.application.getplan.GetPlanQuery;
import com.buildingos.subscription.plan.application.getplan.GetPlanUseCase;
import com.buildingos.subscription.plan.application.listplans.ListPlansUseCase;
import com.buildingos.subscription.plan.application.retireplan.RetirePlanCommand;
import com.buildingos.subscription.plan.application.retireplan.RetirePlanUseCase;
import com.buildingos.subscription.plan.application.updateplan.UpdatePlanUseCase;
import com.buildingos.subscription.plan.presentation.rest.mapper.PlanRequestMapper;
import com.buildingos.subscription.plan.presentation.rest.request.PlanRequest;
import com.buildingos.subscription.plan.presentation.rest.response.PlanResponse;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
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

@RestController
@RequestMapping("/api/v1/platform/subscription-plans")
public class SubscriptionPlanController {
    private final CreatePlanUseCase create;
    private final UpdatePlanUseCase update;
    private final RetirePlanUseCase retire;
    private final GetPlanUseCase get;
    private final ListPlansUseCase list;

    public SubscriptionPlanController(CreatePlanUseCase create, UpdatePlanUseCase update, RetirePlanUseCase retire,
            GetPlanUseCase get, ListPlansUseCase list) {
        this.create = create;
        this.update = update;
        this.retire = retire;
        this.get = get;
        this.list = list;
    }

    @GetMapping
    public ApiEnvelope<List<PlanResponse>> list(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return ApiEnvelope.of(list.execute(CurrentActor.from(jwt)).stream().map(PlanResponse::of).toList(),
                CorrelationFilter.traceId(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<PlanResponse> create(@AuthenticationPrincipal Jwt jwt, @RequestBody PlanRequest body,
            HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        actor.requireRevenueAdmin();
        return ApiEnvelope.of(PlanResponse.of(create.execute(actor, PlanRequestMapper.toCreate(body))),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/{planId}")
    public ApiEnvelope<PlanResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId,
            HttpServletRequest request) {
        return ApiEnvelope.of(PlanResponse.of(get.execute(CurrentActor.from(jwt), new GetPlanQuery(planId))),
                CorrelationFilter.traceId(request));
    }

    @PutMapping("/{planId}")
    public ApiEnvelope<PlanResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId,
            @RequestBody PlanRequest body, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        actor.requireRevenueAdmin();
        return ApiEnvelope.of(PlanResponse.of(update.execute(actor, PlanRequestMapper.toUpdate(planId, body))),
                CorrelationFilter.traceId(request));
    }

    @PostMapping("/{planId}/retire")
    public ApiEnvelope<PlanResponse> retire(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID planId,
            HttpServletRequest request) {
        return ApiEnvelope.of(PlanResponse.of(retire.execute(CurrentActor.from(jwt), new RetirePlanCommand(planId))),
                CorrelationFilter.traceId(request));
    }
}
