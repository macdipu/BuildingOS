package com.buildingos.backoffice.supportsession.presentation.rest;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.presentation.rest.CurrentActor;
import com.buildingos.backoffice.shared.presentation.rest.Enums;
import com.buildingos.backoffice.supportsession.application.DecideElevatedApprovalCommand;
import com.buildingos.backoffice.supportsession.application.approveelevatedapproval.ApproveElevatedApprovalUseCase;
import com.buildingos.backoffice.supportsession.application.denyelevatedapproval.DenyElevatedApprovalUseCase;
import com.buildingos.backoffice.supportsession.application.listelevatedapprovals.ListElevatedApprovalsQuery;
import com.buildingos.backoffice.supportsession.application.listelevatedapprovals.ListElevatedApprovalsUseCase;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalStatus;
import com.buildingos.backoffice.supportsession.presentation.rest.request.SupportReasonRequest;
import com.buildingos.backoffice.supportsession.presentation.rest.response.ElevatedApprovalResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Elevated (four-eyes) approvals of high-risk support scopes (§149.12, D-10, D-36d). */
@RestController
@RequestMapping("/api/v1/platform/backoffice/elevated-approvals")
public class ElevatedApprovalController {
    private final ListElevatedApprovalsUseCase list;
    private final ApproveElevatedApprovalUseCase approve;
    private final DenyElevatedApprovalUseCase deny;

    public ElevatedApprovalController(ListElevatedApprovalsUseCase list, ApproveElevatedApprovalUseCase approve,
            DenyElevatedApprovalUseCase deny) {
        this.list = list;
        this.approve = approve;
        this.deny = deny;
    }

    @GetMapping
    public ApiEnvelope<List<ElevatedApprovalResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        var result = list.execute(CurrentActor.from(jwt), new ListElevatedApprovalsQuery(
                Enums.parseOptional(ElevatedApprovalStatus.class, status, "status"), page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(ElevatedApprovalResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }

    @PostMapping("/{approvalId}/approve")
    public ApiEnvelope<ElevatedApprovalResponse> approve(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID approvalId, @RequestBody(required = false) SupportReasonRequest body,
            HttpServletRequest request) {
        return decide(jwt, approvalId, body, approve::execute, request);
    }

    @PostMapping("/{approvalId}/deny")
    public ApiEnvelope<ElevatedApprovalResponse> deny(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID approvalId, @RequestBody(required = false) SupportReasonRequest body,
            HttpServletRequest request) {
        return decide(jwt, approvalId, body, deny::execute, request);
    }

    private static ApiEnvelope<ElevatedApprovalResponse> decide(Jwt jwt, UUID approvalId, SupportReasonRequest body,
            BiFunction<Actor, DecideElevatedApprovalCommand, ElevatedApprovalRequest> action,
            HttpServletRequest request) {
        var decided = action.apply(CurrentActor.from(jwt),
                new DecideElevatedApprovalCommand(approvalId, body == null ? null : body.reason()));
        return ApiEnvelope.of(ElevatedApprovalResponse.of(decided), CorrelationFilter.traceId(request));
    }
}
