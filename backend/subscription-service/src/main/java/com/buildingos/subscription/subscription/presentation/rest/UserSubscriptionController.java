package com.buildingos.subscription.subscription.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
import com.buildingos.subscription.shared.presentation.rest.Enums;
import com.buildingos.subscription.subscription.application.getusersubscription.GetUserSubscriptionQuery;
import com.buildingos.subscription.subscription.application.getusersubscription.GetUserSubscriptionUseCase;
import com.buildingos.subscription.subscription.application.grantsubscription.GrantSubscriptionCommand;
import com.buildingos.subscription.subscription.application.grantsubscription.GrantSubscriptionUseCase;
import com.buildingos.subscription.subscription.presentation.rest.request.SubscribeRequest;
import com.buildingos.subscription.subscription.presentation.rest.response.SubscriptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Back-office: put a user on a plan, or inspect a user's subscription. */
@RestController
@RequestMapping("/api/v1/platform/users/{userId}/subscription")
public class UserSubscriptionController {
    private final GrantSubscriptionUseCase grant;
    private final GetUserSubscriptionUseCase get;

    public UserSubscriptionController(GrantSubscriptionUseCase grant, GetUserSubscriptionUseCase get) {
        this.grant = grant;
        this.get = get;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<SubscriptionResponse> grant(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @RequestBody SubscribeRequest body, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        actor.requireRevenueAdmin();
        var started = grant.execute(actor, new GrantSubscriptionCommand(userId, body.requiredPlanId(),
                Enums.parse(BillingCycle.class, body.billingCycle(), "billingCycle")));
        var view = get.execute(actor, new GetUserSubscriptionQuery(userId));
        return ApiEnvelope.of(SubscriptionResponse.of(started, view.effectiveEntitlements().toKeys()),
                CorrelationFilter.traceId(request));
    }

    @GetMapping
    public ApiEnvelope<SubscriptionResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            HttpServletRequest request) {
        var view = get.execute(CurrentActor.from(jwt), new GetUserSubscriptionQuery(userId));
        return ApiEnvelope.of(SubscriptionResponse.of(view.subscription(), view.effectiveEntitlements().toKeys()),
                CorrelationFilter.traceId(request));
    }
}
