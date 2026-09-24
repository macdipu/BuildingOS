package com.buildingos.auth.auth.presentation.rest;

import com.buildingos.auth.auth.application.provisionuser.ProvisionUserCommand;
import com.buildingos.auth.auth.application.provisionuser.ProvisionUserUseCase;
import com.buildingos.auth.auth.application.provisionuser.ProvisioningNotPermittedException;
import com.buildingos.auth.auth.presentation.rest.request.ProvisionUserRequest;
import com.buildingos.auth.auth.presentation.rest.response.ProvisionedUserResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.platform.web.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only: not routed by the gateway. The caller relays a platform admin's access token. */
@RestController
public class UserProvisioningController {
    private static final Logger log = LoggerFactory.getLogger(UserProvisioningController.class);
    private final ProvisionUserUseCase provision;

    public UserProvisioningController(ProvisionUserUseCase provision) {
        this.provision = provision;
    }

    @ExceptionHandler(ProvisioningNotPermittedException.class)
    public ResponseEntity<ApiError> notPermitted(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("ACCESS_DENIED", "Access is denied", CorrelationFilter.traceId(request)));
    }

    @PostMapping("/internal/users/provision")
    public ApiEnvelope<ProvisionedUserResponse> provision(@AuthenticationPrincipal Jwt jwt,
            @RequestBody ProvisionUserRequest body, HttpServletRequest request) {
        List<String> roles = jwt.getClaimAsStringList("platform_roles");
        var result = provision.execute(new ProvisionUserCommand(roles == null ? Set.of() : Set.copyOf(roles),
                body.phone()));
        log.info("user_provisioned actor={} user={}", jwt.getSubject(), result.userId());
        return ApiEnvelope.of(new ProvisionedUserResponse(result.userId(), result.phone()),
                CorrelationFilter.traceId(request));
    }
}
