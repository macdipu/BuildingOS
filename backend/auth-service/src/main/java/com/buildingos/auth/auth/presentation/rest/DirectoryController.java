package com.buildingos.auth.auth.presentation.rest;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.application.getdirectoryuser.GetDirectoryUserQuery;
import com.buildingos.auth.auth.application.getdirectoryuser.GetDirectoryUserUseCase;
import com.buildingos.auth.auth.presentation.rest.response.DirectoryUserResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.platform.web.response.ApiError;
import com.buildingos.platform.web.security.ServicePrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only (D-38): not routed by the gateway; requires a service token with {@code directory.read}. */
@RestController
public class DirectoryController {
    private final GetDirectoryUserUseCase get;

    public DirectoryController(GetDirectoryUserUseCase get) {
        this.get = get;
    }

    @ExceptionHandler(PlatformRoleManagementNotPermittedException.class)
    public ResponseEntity<ApiError> notPermitted(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("ACCESS_DENIED", "Access is denied", CorrelationFilter.traceId(request)));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> notFound(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("USER_NOT_FOUND", "User not found", CorrelationFilter.traceId(request)));
    }

    @GetMapping("/internal/directory/users/{id}")
    public ApiEnvelope<DirectoryUserResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            HttpServletRequest request) {
        var user = get.execute(new GetDirectoryUserQuery(ServicePrincipal.scopes(jwt), id));
        return ApiEnvelope.of(DirectoryUserResponse.of(user), CorrelationFilter.traceId(request));
    }
}
