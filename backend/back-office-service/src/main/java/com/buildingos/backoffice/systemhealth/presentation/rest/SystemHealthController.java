package com.buildingos.backoffice.systemhealth.presentation.rest;

import com.buildingos.backoffice.shared.presentation.rest.CurrentActor;
import com.buildingos.backoffice.systemhealth.application.getsystemhealth.GetSystemHealthUseCase;
import com.buildingos.backoffice.systemhealth.presentation.rest.response.SystemHealthResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** System > System Health (BOC-08): SUPER_ADMIN only. */
@RestController
public class SystemHealthController {
    private final GetSystemHealthUseCase getSystemHealth;

    public SystemHealthController(GetSystemHealthUseCase getSystemHealth) {
        this.getSystemHealth = getSystemHealth;
    }

    @GetMapping("/api/v1/platform/backoffice/system-health")
    public ApiEnvelope<SystemHealthResponse> systemHealth(@AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiEnvelope.of(SystemHealthResponse.of(getSystemHealth.execute(CurrentActor.from(jwt))),
                CorrelationFilter.traceId(request));
    }
}
