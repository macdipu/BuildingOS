package com.buildingos.backoffice.platformuser.presentation.rest;

import com.buildingos.backoffice.platformuser.application.assignplatformrole.AssignPlatformRoleCommand;
import com.buildingos.backoffice.platformuser.application.assignplatformrole.AssignPlatformRoleUseCase;
import com.buildingos.backoffice.platformuser.application.getplatformuser.GetPlatformUserUseCase;
import com.buildingos.backoffice.platformuser.application.listplatformusers.ListPlatformUsersQuery;
import com.buildingos.backoffice.platformuser.application.listplatformusers.ListPlatformUsersUseCase;
import com.buildingos.backoffice.platformuser.application.revokeplatformrole.RevokePlatformRoleCommand;
import com.buildingos.backoffice.platformuser.application.revokeplatformrole.RevokePlatformRoleUseCase;
import com.buildingos.backoffice.platformuser.presentation.rest.request.AssignPlatformRoleRequest;
import com.buildingos.backoffice.platformuser.presentation.rest.response.PlatformUserResponse;
import com.buildingos.backoffice.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Users > platform users and platform role assignment (BOC-05): a token-relaying proxy over auth-service F6-T2. */
@RestController
@RequestMapping("/api/v1/platform/backoffice/users")
public class PlatformUserController {
    private final ListPlatformUsersUseCase list;
    private final GetPlatformUserUseCase get;
    private final AssignPlatformRoleUseCase assign;
    private final RevokePlatformRoleUseCase revoke;

    public PlatformUserController(ListPlatformUsersUseCase list, GetPlatformUserUseCase get,
            AssignPlatformRoleUseCase assign, RevokePlatformRoleUseCase revoke) {
        this.list = list;
        this.get = get;
        this.assign = assign;
        this.revoke = revoke;
    }

    @GetMapping
    public ApiEnvelope<List<PlatformUserResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String query, @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = list.execute(CurrentActor.from(jwt), new ListPlatformUsersQuery(query, role, page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(PlatformUserResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/{userId}")
    public ApiEnvelope<PlatformUserResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            HttpServletRequest request) {
        return ApiEnvelope.of(PlatformUserResponse.of(get.execute(CurrentActor.from(jwt), userId)),
                CorrelationFilter.traceId(request));
    }

    @PostMapping("/{userId}/platform-roles")
    public ApiEnvelope<PlatformUserResponse> assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @RequestBody AssignPlatformRoleRequest body, HttpServletRequest request) {
        var user = assign.execute(CurrentActor.from(jwt), new AssignPlatformRoleCommand(userId, body.role()));
        return ApiEnvelope.of(PlatformUserResponse.of(user), CorrelationFilter.traceId(request));
    }

    @DeleteMapping("/{userId}/platform-roles/{role}")
    public ApiEnvelope<PlatformUserResponse> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
            @PathVariable String role, HttpServletRequest request) {
        var user = revoke.execute(CurrentActor.from(jwt), new RevokePlatformRoleCommand(userId, role));
        return ApiEnvelope.of(PlatformUserResponse.of(user), CorrelationFilter.traceId(request));
    }
}
