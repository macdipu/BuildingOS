package com.buildingos.subscription.plan.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.plan.application.listselfserviceplans.ListSelfServicePlansUseCase;
import com.buildingos.subscription.plan.presentation.rest.response.PlanResponse;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SelfServicePlanController {
    private final ListSelfServicePlansUseCase list;

    public SelfServicePlanController(ListSelfServicePlansUseCase list) { this.list = list; }

    @GetMapping("/api/v1/me/plans")
    public ApiEnvelope<List<PlanResponse>> plans(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return ApiEnvelope.of(list.execute(CurrentActor.from(jwt)).stream().map(PlanResponse::of).toList(),
                CorrelationFilter.traceId(request));
    }
}
