package com.buildingos.backoffice.supportsession.presentation.rest;

import com.buildingos.backoffice.shared.presentation.rest.CurrentActor;
import com.buildingos.backoffice.shared.presentation.rest.Enums;
import com.buildingos.backoffice.supportsession.application.checksupportscope.CheckSupportScopeQuery;
import com.buildingos.backoffice.supportsession.application.checksupportscope.CheckSupportScopeUseCase;
import com.buildingos.backoffice.supportsession.application.endsupportsession.EndSupportSessionCommand;
import com.buildingos.backoffice.supportsession.application.endsupportsession.EndSupportSessionUseCase;
import com.buildingos.backoffice.supportsession.application.getsupportsession.GetSupportSessionQuery;
import com.buildingos.backoffice.supportsession.application.getsupportsession.GetSupportSessionUseCase;
import com.buildingos.backoffice.supportsession.application.listsupportsessions.ListSupportSessionsQuery;
import com.buildingos.backoffice.supportsession.application.listsupportsessions.ListSupportSessionsUseCase;
import com.buildingos.backoffice.supportsession.application.requestelevatedapproval.RequestElevatedApprovalCommand;
import com.buildingos.backoffice.supportsession.application.requestelevatedapproval.RequestElevatedApprovalUseCase;
import com.buildingos.backoffice.supportsession.application.startsupportsession.StartSupportSessionCommand;
import com.buildingos.backoffice.supportsession.application.startsupportsession.StartSupportSessionUseCase;
import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import com.buildingos.backoffice.supportsession.presentation.rest.request.RequestElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.presentation.rest.request.StartSupportSessionRequest;
import com.buildingos.backoffice.supportsession.presentation.rest.request.SupportReasonRequest;
import com.buildingos.backoffice.supportsession.presentation.rest.response.ElevatedApprovalResponse;
import com.buildingos.backoffice.supportsession.presentation.rest.response.ScopeCheckResponse;
import com.buildingos.backoffice.supportsession.presentation.rest.response.SupportSessionDetailResponse;
import com.buildingos.backoffice.supportsession.presentation.rest.response.SupportSessionResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Support sessions (BOC-07, D-36) for support agents and platform admins. */
@RestController
@RequestMapping("/api/v1/platform/backoffice/support-sessions")
public class SupportSessionController {
    private final StartSupportSessionUseCase start;
    private final ListSupportSessionsUseCase list;
    private final GetSupportSessionUseCase get;
    private final EndSupportSessionUseCase end;
    private final RequestElevatedApprovalUseCase requestApproval;
    private final CheckSupportScopeUseCase checkScope;

    public SupportSessionController(StartSupportSessionUseCase start, ListSupportSessionsUseCase list,
            GetSupportSessionUseCase get, EndSupportSessionUseCase end,
            RequestElevatedApprovalUseCase requestApproval, CheckSupportScopeUseCase checkScope) {
        this.start = start;
        this.list = list;
        this.get = get;
        this.end = end;
        this.requestApproval = requestApproval;
        this.checkScope = checkScope;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<SupportSessionDetailResponse> start(@AuthenticationPrincipal Jwt jwt,
            @RequestBody StartSupportSessionRequest body, HttpServletRequest request) {
        var created = start.execute(CurrentActor.from(jwt), new StartSupportSessionCommand(body.targetUserId(),
                body.buildingId(), Enums.parseList(SupportScope.class, body.permissionScope(), "permissionScope"),
                body.reason(), body.expiresAt()));
        return ApiEnvelope.of(SupportSessionDetailResponse.of(created), CorrelationFilter.traceId(request));
    }

    @GetMapping
    public ApiEnvelope<List<SupportSessionResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) UUID platformUserId,
            @RequestParam(required = false) UUID targetUserId, @RequestParam(required = false) UUID buildingId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = list.execute(CurrentActor.from(jwt),
                new ListSupportSessionsQuery(active, platformUserId, targetUserId, buildingId, page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(SupportSessionResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/{sessionId}")
    public ApiEnvelope<SupportSessionDetailResponse> get(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId, HttpServletRequest request) {
        var details = get.execute(CurrentActor.from(jwt), new GetSupportSessionQuery(sessionId));
        return ApiEnvelope.of(SupportSessionDetailResponse.of(details), CorrelationFilter.traceId(request));
    }

    @PostMapping("/{sessionId}/end")
    public ApiEnvelope<SupportSessionDetailResponse> end(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId, @RequestBody(required = false) SupportReasonRequest body,
            HttpServletRequest request) {
        var ended = end.execute(CurrentActor.from(jwt),
                new EndSupportSessionCommand(sessionId, body == null ? null : body.reason()));
        return ApiEnvelope.of(SupportSessionDetailResponse.of(ended), CorrelationFilter.traceId(request));
    }

    @PostMapping("/{sessionId}/elevated-approvals")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<ElevatedApprovalResponse> requestApproval(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId, @RequestBody RequestElevatedApprovalRequest body,
            HttpServletRequest request) {
        var created = requestApproval.execute(CurrentActor.from(jwt), new RequestElevatedApprovalCommand(sessionId,
                Enums.parseOptional(SupportScope.class, body.scope(), "scope"), body.reason()));
        return ApiEnvelope.of(ElevatedApprovalResponse.of(created), CorrelationFilter.traceId(request));
    }

    @GetMapping("/{sessionId}/scope-check")
    public ApiEnvelope<ScopeCheckResponse> checkScope(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId,
            @RequestParam String scope, HttpServletRequest request) {
        var check = checkScope.execute(CurrentActor.from(jwt),
                new CheckSupportScopeQuery(sessionId, Enums.parseOptional(SupportScope.class, scope, "scope")));
        return ApiEnvelope.of(ScopeCheckResponse.of(check), CorrelationFilter.traceId(request));
    }
}
