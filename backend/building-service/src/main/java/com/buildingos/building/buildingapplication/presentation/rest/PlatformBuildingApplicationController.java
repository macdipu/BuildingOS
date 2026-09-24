package com.buildingos.building.buildingapplication.presentation.rest;

import com.buildingos.building.buildingapplication.application.approveapplication.ApproveApplicationCommand;
import com.buildingos.building.buildingapplication.application.approveapplication.ApproveApplicationUseCase;
import com.buildingos.building.buildingapplication.application.listapplications.ListApplicationsQuery;
import com.buildingos.building.buildingapplication.application.listapplications.ListApplicationsUseCase;
import com.buildingos.building.buildingapplication.application.rejectapplication.RejectApplicationCommand;
import com.buildingos.building.buildingapplication.application.rejectapplication.RejectApplicationUseCase;
import com.buildingos.building.buildingapplication.application.requestinformation.RequestInformationCommand;
import com.buildingos.building.buildingapplication.application.requestinformation.RequestInformationUseCase;
import com.buildingos.building.buildingapplication.application.startreview.StartReviewCommand;
import com.buildingos.building.buildingapplication.application.startreview.StartReviewUseCase;
import com.buildingos.building.buildingapplication.domain.model.ApplicationStatus;
import com.buildingos.building.buildingapplication.presentation.rest.request.ApproveRequest;
import com.buildingos.building.buildingapplication.presentation.rest.request.MessageRequest;
import com.buildingos.building.buildingapplication.presentation.rest.request.ReasonRequest;
import com.buildingos.building.buildingapplication.presentation.rest.response.ApplicationResponse;
import com.buildingos.building.buildingapplication.presentation.rest.response.ApprovalResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.building.shared.presentation.rest.Enums;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Back-office review actions (SUPER_ADMIN or PLATFORM_ADMIN, BA-04/BA-05). */
@RestController
@RequestMapping("/api/v1/platform/building-applications")
public class PlatformBuildingApplicationController {
    private final ListApplicationsUseCase list;
    private final StartReviewUseCase startReview;
    private final RequestInformationUseCase requestInformation;
    private final RejectApplicationUseCase reject;
    private final ApproveApplicationUseCase approve;

    public PlatformBuildingApplicationController(ListApplicationsUseCase list, StartReviewUseCase startReview,
            RequestInformationUseCase requestInformation, RejectApplicationUseCase reject,
            ApproveApplicationUseCase approve) {
        this.list = list;
        this.startReview = startReview;
        this.requestInformation = requestInformation;
        this.reject = reject;
        this.approve = approve;
    }

    @GetMapping
    public ApiEnvelope<List<ApplicationResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        var result = list.execute(CurrentActor.from(jwt),
                new ListApplicationsQuery(Enums.parseOptional(ApplicationStatus.class, status, "status"), page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(a -> ApplicationResponse.of(a, true)).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }

    @PostMapping("/{applicationId}/start-review")
    public ApiEnvelope<ApplicationResponse> startReview(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, HttpServletRequest request) {
        var changed = startReview.execute(CurrentActor.from(jwt), new StartReviewCommand(applicationId));
        return ApiEnvelope.of(ApplicationResponse.of(changed, true), CorrelationFilter.traceId(request));
    }

    @PostMapping("/{applicationId}/request-information")
    public ApiEnvelope<ApplicationResponse> requestInformation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, @RequestBody MessageRequest body, HttpServletRequest request) {
        var changed = requestInformation.execute(CurrentActor.from(jwt),
                new RequestInformationCommand(applicationId, body.message()));
        return ApiEnvelope.of(ApplicationResponse.of(changed, true), CorrelationFilter.traceId(request));
    }

    @PostMapping("/{applicationId}/reject")
    public ApiEnvelope<ApplicationResponse> reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            @RequestBody ReasonRequest body, HttpServletRequest request) {
        var changed = reject.execute(CurrentActor.from(jwt), new RejectApplicationCommand(applicationId, body.reason()));
        return ApiEnvelope.of(ApplicationResponse.of(changed, true), CorrelationFilter.traceId(request));
    }

    @PostMapping("/{applicationId}/approve")
    public ApiEnvelope<ApprovalResponse> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID applicationId,
            @RequestBody ApproveRequest body, HttpServletRequest request) {
        var result = approve.execute(CurrentActor.from(jwt),
                new ApproveApplicationCommand(applicationId, body.adminPhone(), body.reason()));
        return ApiEnvelope.of(ApprovalResponse.of(result), CorrelationFilter.traceId(request));
    }
}
