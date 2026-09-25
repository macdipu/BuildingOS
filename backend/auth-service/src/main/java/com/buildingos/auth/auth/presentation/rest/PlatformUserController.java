package com.buildingos.auth.auth.presentation.rest;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.application.assignplatformrole.AssignPlatformRoleCommand;
import com.buildingos.auth.auth.application.assignplatformrole.AssignPlatformRoleUseCase;
import com.buildingos.auth.auth.application.getuser.GetUserQuery;
import com.buildingos.auth.auth.application.getuser.GetUserUseCase;
import com.buildingos.auth.auth.application.listusers.ListUsersQuery;
import com.buildingos.auth.auth.application.listusers.ListUsersUseCase;
import com.buildingos.auth.auth.application.revokeplatformrole.RevokePlatformRoleCommand;
import com.buildingos.auth.auth.application.revokeplatformrole.RevokePlatformRoleUseCase;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.presentation.rest.request.AssignPlatformRoleRequest;
import com.buildingos.auth.auth.presentation.rest.response.PlatformUserResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.platform.web.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only: not routed by the gateway. The caller relays a platform admin's access token. */
@RestController
public class PlatformUserController {
    private final ListUsersUseCase list;
    private final GetUserUseCase get;
    private final AssignPlatformRoleUseCase assign;
    private final RevokePlatformRoleUseCase revoke;

    public PlatformUserController(ListUsersUseCase list, GetUserUseCase get, AssignPlatformRoleUseCase assign,
            RevokePlatformRoleUseCase revoke) {
        this.list = list;
        this.get = get;
        this.assign = assign;
        this.revoke = revoke;
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

    @GetMapping("/internal/users")
    public ApiEnvelope<List<PlatformUserResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = list.execute(new ListUsersQuery(callerRoles(jwt), query, parseRole(role), page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(PlatformUserResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/internal/users/{id}")
    public ApiEnvelope<PlatformUserResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            HttpServletRequest request) {
        var user = get.execute(new GetUserQuery(callerRoles(jwt), id));
        return ApiEnvelope.of(PlatformUserResponse.of(user), CorrelationFilter.traceId(request));
    }

    @PostMapping("/internal/users/{id}/platform-roles")
    public ApiEnvelope<PlatformUserResponse> assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody AssignPlatformRoleRequest body, HttpServletRequest request) {
        var user = assign.execute(new AssignPlatformRoleCommand(
                callerId(jwt), callerRoles(jwt), id, parseRole(body.role())));
        return ApiEnvelope.of(PlatformUserResponse.of(user), CorrelationFilter.traceId(request));
    }

    @DeleteMapping("/internal/users/{id}/platform-roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable String role) {
        revoke.execute(new RevokePlatformRoleCommand(callerId(jwt), callerRoles(jwt), id, parseRole(role)));
    }

    private static UUID callerId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException notAUser) {
            throw new PlatformRoleManagementNotPermittedException();
        }
    }

    private static Set<String> callerRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("platform_roles");
        return roles == null ? Set.of() : Set.copyOf(roles);
    }

    private static PlatformRole parseRole(String role) {
        if (role == null) {
            return null;
        }
        try {
            return PlatformRole.valueOf(role);
        } catch (IllegalArgumentException invalidRole) {
            throw new IllegalArgumentException("Unknown platform role: " + role);
        }
    }
}
