package com.buildingos.subscription.subscription.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
import com.buildingos.subscription.shared.presentation.rest.Enums;
import com.buildingos.subscription.subscription.application.getmyentitlements.GetMyEntitlementsUseCase;
import com.buildingos.subscription.subscription.application.selfsubscribe.SelfSubscribeCommand;
import com.buildingos.subscription.subscription.application.selfsubscribe.SelfSubscribeUseCase;
import com.buildingos.subscription.subscription.presentation.rest.request.SubscribeRequest;
import com.buildingos.subscription.subscription.presentation.rest.response.SubscriptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Any logged-in user: own entitlements and self-subscribe (D-22, D-25). */
@RestController
public class MySubscriptionController {
    private final SelfSubscribeUseCase subscribe;
    private final GetMyEntitlementsUseCase entitlements;

    public MySubscriptionController(SelfSubscribeUseCase subscribe, GetMyEntitlementsUseCase entitlements) {
        this.subscribe = subscribe;
        this.entitlements = entitlements;
    }

    @PostMapping("/api/v1/me/subscription")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<SubscriptionResponse> subscribe(@AuthenticationPrincipal Jwt jwt,
            @RequestBody SubscribeRequest body, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        var started = subscribe.execute(actor, new SelfSubscribeCommand(body.requiredPlanId(),
                Enums.parse(BillingCycle.class, body.billingCycle(), "billingCycle")));
        return ApiEnvelope.of(SubscriptionResponse.of(started, entitlements.execute(actor).toKeys()),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/api/v1/me/entitlements")
    public ApiEnvelope<Map<String, Object>> entitlements(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return ApiEnvelope.of(entitlements.execute(CurrentActor.from(jwt)).toKeys(), CorrelationFilter.traceId(request));
    }
}
