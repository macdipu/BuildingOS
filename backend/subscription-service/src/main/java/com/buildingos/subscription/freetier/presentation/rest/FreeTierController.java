package com.buildingos.subscription.freetier.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.freetier.application.getfreetier.GetFreeTierUseCase;
import com.buildingos.subscription.freetier.application.updatefreetier.UpdateFreeTierCommand;
import com.buildingos.subscription.freetier.application.updatefreetier.UpdateFreeTierUseCase;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Body and response are the entitlement map, e.g. {@code {"maintenance.enabled": true}}. */
@RestController
@RequestMapping("/api/v1/platform/free-tier")
public class FreeTierController {
    private final GetFreeTierUseCase get;
    private final UpdateFreeTierUseCase update;

    public FreeTierController(GetFreeTierUseCase get, UpdateFreeTierUseCase update) {
        this.get = get;
        this.update = update;
    }

    @GetMapping
    public ApiEnvelope<Map<String, Object>> get(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return ApiEnvelope.of(get.execute(CurrentActor.from(jwt)).toKeys(), CorrelationFilter.traceId(request));
    }

    @PutMapping
    public ApiEnvelope<Map<String, Object>> update(@AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> entitlements, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        actor.requireRevenueAdmin();
        var saved = update.execute(actor, new UpdateFreeTierCommand(Entitlements.fromKeys(entitlements)));
        return ApiEnvelope.of(saved.toKeys(), CorrelationFilter.traceId(request));
    }
}
