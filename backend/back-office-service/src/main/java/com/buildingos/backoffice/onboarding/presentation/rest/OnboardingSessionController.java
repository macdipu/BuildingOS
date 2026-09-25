package com.buildingos.backoffice.onboarding.presentation.rest;

import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.application.awaitonboardingcustomer.AwaitOnboardingCustomerUseCase;
import com.buildingos.backoffice.onboarding.application.cancelonboardingsession.CancelOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.completeonboardingsession.CompleteOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.getonboardingsession.GetOnboardingSessionQuery;
import com.buildingos.backoffice.onboarding.application.getonboardingsession.GetOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.listonboardingsessions.ListOnboardingSessionsQuery;
import com.buildingos.backoffice.onboarding.application.listonboardingsessions.ListOnboardingSessionsUseCase;
import com.buildingos.backoffice.onboarding.application.startonboardingsession.StartOnboardingSessionCommand;
import com.buildingos.backoffice.onboarding.application.startonboardingsession.StartOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.startonboardingwork.StartOnboardingWorkUseCase;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingScope;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus;
import com.buildingos.backoffice.onboarding.presentation.rest.request.StartOnboardingSessionRequest;
import com.buildingos.backoffice.onboarding.presentation.rest.request.TransitionReasonRequest;
import com.buildingos.backoffice.onboarding.presentation.rest.response.OnboardingSessionResponse;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.presentation.rest.CurrentActor;
import com.buildingos.backoffice.shared.presentation.rest.Enums;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
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

/** Assisted onboarding sessions (BOC-06, D-33) for platform operators and onboarding agents. */
@RestController
@RequestMapping("/api/v1/platform/backoffice/onboarding-sessions")
public class OnboardingSessionController {
    private final StartOnboardingSessionUseCase start;
    private final ListOnboardingSessionsUseCase list;
    private final GetOnboardingSessionUseCase get;
    private final StartOnboardingWorkUseCase startWork;
    private final AwaitOnboardingCustomerUseCase awaitCustomer;
    private final CompleteOnboardingSessionUseCase complete;
    private final CancelOnboardingSessionUseCase cancel;

    public OnboardingSessionController(StartOnboardingSessionUseCase start, ListOnboardingSessionsUseCase list,
            GetOnboardingSessionUseCase get, StartOnboardingWorkUseCase startWork,
            AwaitOnboardingCustomerUseCase awaitCustomer, CompleteOnboardingSessionUseCase complete,
            CancelOnboardingSessionUseCase cancel) {
        this.start = start;
        this.list = list;
        this.get = get;
        this.startWork = startWork;
        this.awaitCustomer = awaitCustomer;
        this.complete = complete;
        this.cancel = cancel;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<OnboardingSessionResponse> start(@AuthenticationPrincipal Jwt jwt,
            @RequestBody StartOnboardingSessionRequest body, HttpServletRequest request) {
        var actor = CurrentActor.from(jwt);
        var created = start.execute(actor, new StartOnboardingSessionCommand(body.buildingId(),
                body.assignedAgentUserId(), Enums.parseList(OnboardingScope.class, body.accessScope(), "accessScope"),
                body.reason(), body.notes(), body.expiresAt()));
        return ApiEnvelope.of(OnboardingSessionResponse.of(created), CorrelationFilter.traceId(request));
    }

    @GetMapping
    public ApiEnvelope<List<OnboardingSessionResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) UUID agentUserId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        var result = list.execute(CurrentActor.from(jwt), new ListOnboardingSessionsQuery(
                Enums.parseOptional(OnboardingSessionStatus.class, status, "status"), buildingId, agentUserId, page,
                size));
        return new ApiEnvelope<>(true, result.items().stream().map(OnboardingSessionResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }

    @GetMapping("/{sessionId}")
    public ApiEnvelope<OnboardingSessionResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId,
            HttpServletRequest request) {
        var session = get.execute(CurrentActor.from(jwt), new GetOnboardingSessionQuery(sessionId));
        return ApiEnvelope.of(OnboardingSessionResponse.of(session), CorrelationFilter.traceId(request));
    }

    @PostMapping("/{sessionId}/start-work")
    public ApiEnvelope<OnboardingSessionResponse> startWork(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId, @RequestBody(required = false) TransitionReasonRequest body,
            HttpServletRequest request) {
        return act(jwt, sessionId, body, startWork::execute, request);
    }

    @PostMapping("/{sessionId}/await-customer")
    public ApiEnvelope<OnboardingSessionResponse> awaitCustomer(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId, @RequestBody(required = false) TransitionReasonRequest body,
            HttpServletRequest request) {
        return act(jwt, sessionId, body, awaitCustomer::execute, request);
    }

    @PostMapping("/{sessionId}/complete")
    public ApiEnvelope<OnboardingSessionResponse> complete(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId, @RequestBody(required = false) TransitionReasonRequest body,
            HttpServletRequest request) {
        return act(jwt, sessionId, body, complete::execute, request);
    }

    @PostMapping("/{sessionId}/cancel")
    public ApiEnvelope<OnboardingSessionResponse> cancel(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID sessionId, @RequestBody(required = false) TransitionReasonRequest body,
            HttpServletRequest request) {
        return act(jwt, sessionId, body, cancel::execute, request);
    }

    private static ApiEnvelope<OnboardingSessionResponse> act(Jwt jwt, UUID sessionId, TransitionReasonRequest body,
            BiFunction<Actor, OnboardingTransitionCommand, AssistedOnboardingSession> action,
            HttpServletRequest request) {
        var changed = action.apply(CurrentActor.from(jwt),
                new OnboardingTransitionCommand(sessionId, body == null ? null : body.reason()));
        return ApiEnvelope.of(OnboardingSessionResponse.of(changed), CorrelationFilter.traceId(request));
    }
}
